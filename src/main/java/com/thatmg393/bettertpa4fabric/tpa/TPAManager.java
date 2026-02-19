package com.thatmg393.bettertpa4fabric.tpa;

import java.util.UUID;

import org.joml.Vector3d;

import com.thatmg393.bettertpa4fabric.BetterTPA4Fabric;
import com.thatmg393.bettertpa4fabric.config.data.ModConfigData;
import com.thatmg393.bettertpa4fabric.tpa.tasks.TeleportTask;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public class TPAManager {
    public static final TPAManager INSTANCE = new TPAManager();

    private static final ChunkTicketType AFTER_TELEPORT = Registry.register(
        Registries.TICKET_TYPE,
        Identifier.of(BetterTPA4Fabric.MOD_ID, "after_teleport"),
        new ChunkTicketType(30, ChunkTicketType.FOR_LOADING)
    );

    private final Object2ObjectOpenHashMap<UUID, TPARequestQueue> incoming    = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<UUID, TeleportTask>    activeTasks = new Object2ObjectOpenHashMap<>();

    private ModConfigData config;

    private TPAManager() { }

    public void updateConfig(ModConfigData config) {
        this.config = config;
    }

    private long getTimeoutMs()  { return config.tpaExpireTime * 1000L; }
    private int getWarmupTicks() { return config.tpaTeleportTime * 20; }

    public void handleTpa(ServerPlayerEntity sender, ServerPlayerEntity target) {
        TPARequestQueue queue = incoming.computeIfAbsent(target.getUuid(), k -> new TPARequestQueue());
        if (queue.size() >= config.tpaRequestLimit) {
            // notify sender: "<target> has too many pending requests"
            return;
        }
        queue.add(new TPARequest(sender.getUuid(), target.getUuid(), TPARequest.Type.TPA, getTimeoutMs()));
        // notify sender: "TPA request sent to <target>"
        // notify target: "<sender> wants to teleport to you. /tpaccept or /tpdeny"
    }

    public void handleTpaHere(ServerPlayerEntity sender, ServerPlayerEntity target) {
        TPARequestQueue queue = incoming.computeIfAbsent(target.getUuid(), k -> new TPARequestQueue());
        if (queue.size() >= config.tpaRequestLimit) {
            // notify sender: "<target> has too many pending requests"
            return;
        }
        queue.add(new TPARequest(sender.getUuid(), target.getUuid(), TPARequest.Type.TPA_HERE, getTimeoutMs()));
        // notify sender: "TPAHere request sent to <target>"
        // notify target: "<sender> wants you to teleport to them. /tpaccept or /tpdeny"
    }

    public void handleAccept(ServerPlayerEntity acceptor, MinecraftServer server) {
        handleAcceptInternal(acceptor, null, server);
    }

    public void handleAcceptFrom(ServerPlayerEntity acceptor, ServerPlayerEntity sender, MinecraftServer server) {
        handleAcceptInternal(acceptor, sender, server);
    }

    private void handleAcceptInternal(ServerPlayerEntity acceptor, ServerPlayerEntity sender, MinecraftServer server) {
        TPARequestQueue queue = incoming.get(acceptor.getUuid());
        if (queue == null) {
            // notify acceptor: "You have no pending requests"
            return;
        }
        TPARequest request = sender == null ? queue.getLatest() : queue.getBySender(sender.getUuid());
        if (request == null) {
            // notify acceptor: "No valid request found"
            return;
        }
        queue.remove(request.sender);
        cleanupIfEmpty(acceptor.getUuid());
        resolveAndTeleport(request, server);
    }

    public void handleDeny(ServerPlayerEntity denier) {
        handleDenyInternal(denier, null);
    }

    public void handleDenyFrom(ServerPlayerEntity denier, ServerPlayerEntity sender) {
        handleDenyInternal(denier, sender);
    }

    private void handleDenyInternal(ServerPlayerEntity denier, ServerPlayerEntity sender) {
        TPARequestQueue queue = incoming.get(denier.getUuid());
        if (queue == null) {
            // notify denier: "You have no pending requests"
            return;
        }
        TPARequest request = sender == null ? queue.getLatest() : queue.getBySender(sender.getUuid());
        if (request == null) {
            // notify denier: "No valid request found"
            return;
        }
        queue.remove(request.sender);
        cleanupIfEmpty(denier.getUuid());
        // notify denier: "Denied <sender>'s request"
        // notify sender: "<denier> denied your request"
    }

    public void handleCancel(ServerPlayerEntity sender, ServerPlayerEntity target) {
        TPARequestQueue queue = incoming.get(target.getUuid());
        if (queue == null) {
            // notify sender: "You have no outgoing request to <target>"
            return;
        }
        TPARequest r = queue.getBySender(sender.getUuid());
        if (r == null) {
            // notify sender: "No active request to <target>"
            return;
        }
        queue.remove(sender.getUuid());
        cleanupIfEmpty(target.getUuid());
        // notify sender: "Cancelled your request to <target>"
        // notify target: "<sender> cancelled their request"
    }

    public void handleTpaBack(ServerPlayerEntity player) {
        PlayerData data = PlayerData.get(player.getUuid());
        if (!data.hasLastLocation()) {
            // notify player: "No previous location to return to"
            return;
        }
        ServerWorld lastWorld = data.lastWorld;
        BlockPos    lastPos   = data.lastPos;
        if (config.oneTimeTPABack) data.clearLastLocation();
        doTeleport(player, null, lastWorld, new Vector3d(lastPos.getX(), lastPos.getY(), lastPos.getZ()));
    }

    public void handleHome(ServerPlayerEntity player) {
        PlayerData data = PlayerData.get(player.getUuid());
        if (!data.hasHome()) {
            // notify player: "You haven't set a home yet. Use /sethome"
            return;
        }
        doTeleport(player, null, data.homeWorld, new Vector3d(data.homePos.getX(), data.homePos.getY(), data.homePos.getZ()));
    }

    public void handleSetHome(ServerPlayerEntity player) {
        PlayerData.get(player.getUuid()).setHome((ServerWorld) player.getEntityWorld(), player.getBlockPos());
        // notify player: "Home set!"
    }

    public void tick() {
        activeTasks.values().removeIf(task -> !task.tick());
    }

    public void onPlayerQuit(ServerPlayerEntity player) {
        incoming.remove(player.getUuid());
        incoming.values().forEach(queue -> queue.remove(player.getUuid()));
        TeleportTask task = activeTasks.get(player.getUuid());
        if (task != null) task.cancel();
    }

    private void resolveAndTeleport(TPARequest request, MinecraftServer server) {
        UUID moverUuid = request.type == TPARequest.Type.TPA ? request.sender : request.target;
        UUID destUuid  = request.type == TPARequest.Type.TPA ? request.target : request.sender;

        ServerPlayerEntity mover = server.getPlayerManager().getPlayer(moverUuid);
        ServerPlayerEntity dest  = server.getPlayerManager().getPlayer(destUuid);

        if (mover == null || dest == null) {
            // notify whichever is online: "The other player is no longer online"
            return;
        }

        doTeleport(mover, dest, (ServerWorld) dest.getEntityWorld(), new Vector3d(dest.getX(), dest.getY(), dest.getZ()));
    }

    private void doTeleport(ServerPlayerEntity player, ServerPlayerEntity dest,
                             ServerWorld destWorld, Vector3d coords) {
        saveLastLocation(player);
        schedule(player, dest, () -> {
            keepChunksLoaded((ServerWorld) player.getEntityWorld(), player.getBlockPos());
            teleportInternal(player, destWorld, coords);
            // notify player: "Teleported!"
            // notify dest: "<player> has teleported to you"
        });
    }

    private void schedule(ServerPlayerEntity player, ServerPlayerEntity dest, Runnable onSuccess) {
        if (activeTasks.containsKey(player.getUuid())) {
            // notify player: "You are already teleporting"
            return;
        }
        activeTasks.put(player.getUuid(), new TeleportTask(player, dest, getWarmupTicks(), onSuccess));
        // notify player: "Teleporting in " + config.tpaTeleportTime + "s, don't move..."
        // notify dest: "<player> is teleporting to you in " + config.tpaTeleportTime + "s..."
    }

    private void saveLastLocation(ServerPlayerEntity player) {
        PlayerData.get(player.getUuid()).saveLastLocation((ServerWorld) player.getEntityWorld(), player.getBlockPos());
    }

    private void cleanupIfEmpty(UUID target) {
        TPARequestQueue queue = incoming.get(target);
        if (queue != null && queue.isEmpty()) incoming.remove(target);
    }

    private void teleportInternal(ServerPlayerEntity player, ServerWorld world, Vector3d coords) {
        player.teleport(
            world,
            coords.x, coords.y, coords.z,
            PositionFlag.DELTA,
            player.getYaw(), player.getPitch(),
            false
        );
    }

    private void keepChunksLoaded(ServerWorld world, BlockPos pos) {
        world.getChunkManager().addTicket(AFTER_TELEPORT, new ChunkPos(pos), 3);
    }
}
