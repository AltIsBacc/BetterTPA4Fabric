package com.thatmg393.bettertpa4fabric.tpa;

import static com.thatmg393.bettertpa4fabric.utils.MCTextUtils.fromLang;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkTicketType;

import java.util.Comparator;
import java.util.UUID;

import javax.lang.model.element.TypeParameterElement;

import com.thatmg393.bettertpa4fabric.config.ModConfigManager;
import com.thatmg393.bettertpa4fabric.config.data.ModConfigData;

public class TPAManager {
    public static final TPAManager INSTANCE = new TPAManager();

    private static final ChunkTicketType<ChunkPos> AFTER_TELEPORT = ChunkTicketType.create("after_teleport", Comparator.comparingLong(ChunkPos::toLong), 30);
    
    private final Object2ObjectOpenHashMap<UUID, RequestQueue> incoming = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<UUID, TeleportTask> activeTasks = new Object2ObjectOpenHashMap<>();

    private ModConfigData config;

    private TPAManager() { }

    public void updateConfig(ModConfigData config) {
        this.config = config;
    }

    public void handleTpa(ServerPlayerEntity sender, ServerPlayerEntity target) {
        incoming.computeIfAbsent(target.getUuid(), k -> new RequestQueue())
                .add(new TpaRequest(sender.getUuid(), target.getUuid(), TpaRequest.Type.TPA, TIMEOUT_MS));
        // notify sender: "TPA request sent to <target>"
        // notify target: "<sender> wants to teleport to you. /tpaccept or /tpdeny"
    }

    public void handleTpaHere(ServerPlayerEntity sender, ServerPlayerEntity target) {
        incoming.computeIfAbsent(target.getUuid(), k -> new RequestQueue())
                .add(new TpaRequest(sender.getUuid(), target.getUuid(), TpaRequest.Type.TPA_HERE, TIMEOUT_MS));
        // notify sender: "TPAHere request sent to <target>"
        // notify target: "<sender> wants you to teleport to them. /tpaccept or /tpdeny"
    }

    public void handleAccept(ServerPlayerEntity acceptor, MinecraftServer server) {
        RequestQueue queue = incoming.get(acceptor.getUuid());
        if (queue == null) {
            // notify acceptor: "You have no pending requests"
            return;
        }
        TpaRequest request = queue.getLatest();
        if (request == null) {
            // notify acceptor: "Your requests have all expired"
            return;
        }
        queue.remove(request.sender);
        cleanupIfEmpty(acceptor.getUuid());
        teleport(request, server);
    }

    public void handleAcceptFrom(ServerPlayerEntity acceptor, ServerPlayerEntity sender, MinecraftServer server) {
        RequestQueue queue = incoming.get(acceptor.getUuid());
        if (queue == null) {
            // notify acceptor: "You have no pending requests"
            return;
        }
        TpaRequest request = queue.getBySender(sender.getUuid());
        if (request == null) {
            // notify acceptor: "No request from <sender> or it expired"
            return;
        }
        queue.remove(sender.getUuid());
        cleanupIfEmpty(acceptor.getUuid());
        teleport(request, server);
    }

    public void handleDeny(ServerPlayerEntity denier, MinecraftServer server) {
        RequestQueue queue = incoming.get(denier.getUuid());
        if (queue == null) {
            // notify denier: "You have no pending requests"
            return;
        }
        TpaRequest request = queue.getLatest();
        if (request == null) {
            // notify denier: "Your requests have all expired"
            return;
        }
        queue.remove(request.sender);
        cleanupIfEmpty(denier.getUuid());
        // notify denier: "Denied <sender>'s request"
        // notify sender: "<denier> denied your request"
    }

    public void handleCancel(ServerPlayerEntity sender, ServerPlayerEntity target) {
        RequestQueue queue = incoming.get(target.getUuid());
        if (queue == null) {
            // notify sender: "You have no outgoing request to <target>"
            return;
        }
        TpaRequest r = queue.getBySender(sender.getUuid());
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
        BlockPos lastPos = data.lastPos;
        saveLastLocation(player);
        schedule(player, null, () -> {
            keepChunksLoaded(player.getServerWorld(), player.getBlockPos());
            player.teleport(lastWorld, lastPos.getX(), lastPos.getY(), lastPos.getZ(),
                    player.getYaw(), player.getPitch());
            teleportPets(player, lastWorld, lastPos.getX(), lastPos.getY(), lastPos.getZ());
            // notify player: "Teleported back to your previous location"
        });
    }

    public void handleHome(ServerPlayerEntity player) {
        PlayerData data = PlayerData.get(player.getUuid());
        if (!data.hasHome()) {
            // notify player: "You haven't set a home yet. Use /sethome"
            return;
        }
        ServerWorld homeWorld = data.homeWorld;
        BlockPos homePos = data.homePos;
        saveLastLocation(player);
        schedule(player, null, () -> {
            keepChunksLoaded(player.getServerWorld(), player.getBlockPos());
            player.teleport(homeWorld, homePos.getX(), homePos.getY(), homePos.getZ(),
                    player.getYaw(), player.getPitch());
            teleportPets(player, homeWorld, homePos.getX(), homePos.getY(), homePos.getZ());
            // notify player: "Teleported to your home"
        });
    }

    public void handleSetHome(ServerPlayerEntity player) {
        PlayerData.get(player.getUuid()).setHome(player.getServerWorld(), player.getBlockPos());
        // notify player: "Home set!"
    }

    public void tick() {
        activeTasks.values().removeIf(task -> !task.tick());
    }

    public void onPlayerQuit(ServerPlayerEntity player) {
        incoming.remove(player.getUuid());
        incoming.values().forEach(queue -> queue.remove(player.getUuid()));
        cancelTask(player.getUuid());
    }



    private void teleport(TpaRequest request, MinecraftServer server) {
        UUID moverUuid = request.type == TpaRequest.Type.TPA ? request.sender : request.target;
        UUID destUuid = request.type == TpaRequest.Type.TPA ? request.target : request.sender;

        ServerPlayerEntity mover = server.getPlayerManager().getPlayer(moverUuid);
        ServerPlayerEntity dest = server.getPlayerManager().getPlayer(destUuid);

        if (mover == null || dest == null) {
            // notify whichever is online: "The other player is no longer online"
            return;
        }

        saveLastLocation(mover);

        schedule(mover, dest, () -> {
            keepChunksLoaded(mover.getServerWorld(), mover.getBlockPos());
            mover.teleport(
                dest.getServerWorld(),
                dest.getX(), dest.getY(), dest.getZ(),
                mover.getYaw(), mover.getPitch()
            );
            // notify mover: "Teleported to <dest>"
            // notify dest: "<mover> has teleported to you"
        });
    }

    private void versionIndependentTeleport(
        ServerPlayerEntity mover,
        ServerPlayerEntity dest
    ) {
        throw new IllegalStateException("teleport func not overriden!");
    }

    private void schedule(ServerPlayerEntity player, ServerPlayerEntity dest, Runnable onComplete) {
        cancelTask(player.getUuid());
        activeTasks.put(player.getUuid(), new TeleportTask(player, dest, onComplete));
        // notify player: "Teleporting in 3 seconds, don't move..."
        // notify dest: "<player> is teleporting to you in 3 seconds..."
    }

    private void cancelTask(UUID uuid) {
        TeleportTask task = activeTasks.remove(uuid);
        if (task != null)
            task.cancel();
    }

    private void saveLastLocation(ServerPlayerEntity player) {
        PlayerData.get(player.getUuid()).saveLastLocation(player.getServerWorld(), player.getBlockPos());
    }

    private void cleanupIfEmpty(UUID target) {
        RequestQueue queue = incoming.get(target);
        if (queue != null && queue.isEmpty())
            incoming.remove(target);
    }

    private void keepChunksLoaded(ServerWorld world, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        world.getChunkManager().addTicket(AFTER_TELEPORT, chunkPos, 3, chunkPos);
    }

    private static class RequestQueue {
        private final Object2ObjectLinkedOpenHashMap<UUID, TpaRequest> bySender = new Object2ObjectLinkedOpenHashMap<>();

        void add(TpaRequest r) {
            bySender.put(r.sender, r);
        }

        void remove(UUID sender) {
            bySender.remove(sender);
        }

        boolean isEmpty() {
            return bySender.isEmpty();
        }

        TpaRequest getLatest() {
            if (bySender.isEmpty())
                return null;
            TpaRequest r = bySender.get(bySender.lastKey());
            return r.isExpired() ? null : r;
        }

        TpaRequest getBySender(UUID sender) {
            TpaRequest r = bySender.get(sender);
            return (r != null && !r.isExpired()) ? r : null;
        }

        void purgeExpired() {
            bySender.values().removeIf(TpaRequest::isExpired);
        }
    }

    private class TeleportTask {
        private final ServerPlayerEntity player;
        private final ServerPlayerEntity dest;
        private final BlockPos startPos;
        private final Runnable onComplete;
        private int ticksRemaining = WARMUP_TICKS;
        private boolean cancelled = false;

        TeleportTask(ServerPlayerEntity player, ServerPlayerEntity dest, Runnable onComplete) {
            this.player = player;
            this.dest = dest;
            this.startPos = player.getBlockPos();
            this.onComplete = onComplete;
        }

        boolean tick() {
            if (cancelled)
                return false;

            if (!player.getBlockPos().equals(startPos)) {
                cancelled = true;
                // notify player: "You moved! Teleport cancelled"
                // notify dest: "<player> moved, teleport cancelled"
                return false;
            }

            if (--ticksRemaining <= 0) {
                onComplete.run();
                return false;
            }

            // notify player: ticksRemaining / 20 + "s remaining..." (every 20 ticks)
            return true;
        }

        void cancel() {
            cancelled = true;
            // notify player: "Teleport cancelled"
            // notify dest: "Teleport cancelled"
        }
    }
}
