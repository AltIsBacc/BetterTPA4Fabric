package com.thatmg393.bettertpa4fabric.tpa;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.thatmg393.bettertpa4fabric.tpa.data.PlayerData;
import com.thatmg393.bettertpa4fabric.tpa.request.TPABackRequest;
import com.thatmg393.bettertpa4fabric.tpa.request.TPAHereRequest;
import com.thatmg393.bettertpa4fabric.tpa.request.TPARequest;
import com.thatmg393.bettertpa4fabric.tpa.request.base.BaseRequest;
import com.thatmg393.bettertpa4fabric.tpa.tickable.TickableTaskProcessor;
import com.thatmg393.bettertpa4fabric.tpa.tickable.task.StaleRequestsCleanerTask;
import com.thatmg393.bettertpa4fabric.tpa.tickable.task.base.TickableTask;
import com.thatmg393.bettertpa4fabric.utils.MCTextUtils;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class TeleportManager {
    public static final TeleportManager INSTANCE = new TeleportManager();

    private final Object2ObjectOpenHashMap<UUID, PlayerData> playerDatas = new Object2ObjectOpenHashMap<>();
    private final TickableTaskProcessor<TickableTask> tickableTasks = new TickableTaskProcessor<>();

    public void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tickableTasks.doTick());
        ServerPlayerEvents.LEAVE.register(player -> playerDatas.remove(player.getUuid()));

        tickableTasks.putTask(new StaleRequestsCleanerTask());
    }

    public int teleportTo(ServerPlayerEntity sender, ServerPlayerEntity receiver) {
        PlayerData receiverData = getPlayerData(receiver.getUuid());
        receiverData.teleportRequests.add(
            sender.getUuid(),
            new TPARequest(sender, receiver)
        );

        sender.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.tpa.sent", receiver.getName().getString()));
        receiver.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.tpa.received", sender.getName().getString()));
        return 1;
    }

    public int teleportHere(ServerPlayerEntity sender, ServerPlayerEntity receiver) {
        PlayerData receiverData = getPlayerData(receiver.getUuid());
        receiverData.teleportRequests.add(
            sender.getUuid(),
            new TPAHereRequest(sender, receiver)
        );

        sender.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.tpahere.sent", receiver.getName().getString()));
        receiver.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.tpahere.received", sender.getName().getString()));
        return 1;
    }

    public int teleportBack(ServerPlayerEntity player) {
        PlayerData playerData = getPlayerData(player.getUuid());

        if (playerData.previousTeleportPosition == null) {
            player.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.error.no_back_location"));
            return 0;
        }

        tickableTasks.putTask(
            new TPABackRequest(player, playerData.previousTeleportPosition).accept()
        );
        return 1;
    }

    public int acceptTeleport(ServerPlayerEntity accepter, @Nullable ServerPlayerEntity from) {
        PlayerData accepterData = getPlayerData(accepter.getUuid());
        BaseRequest request;

        if (from == null) {
            request = accepterData.teleportRequests.consume();
            if (request == null) {
                accepter.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.error.no_incoming_requests"));
                return 0;
            }
            from = request.getRequester();
        } else {
            request = accepterData.teleportRequests.consumeByKey(from.getUuid());
            if (request == null) {
                accepter.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.error.no_request_from_player", from.getName().getString()));
                return 0;
            }
        }

        accepter.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.tpa.accepted.receiver", from.getName().getString()));
        from.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.tpa.accepted.requester", accepter.getName().getString()));
        tickableTasks.putTask(request.accept());

        return 1;
    }

    public int denyTeleport(ServerPlayerEntity denier, @Nullable ServerPlayerEntity from) {
        PlayerData denierData = getPlayerData(denier.getUuid());
        BaseRequest request;

        if (from == null) {
            request = denierData.teleportRequests.consume();
            if (request == null) {
                denier.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.error.no_incoming_requests"));
                return 0;
            }
            from = request.getRequester();
        } else {
            request = denierData.teleportRequests.consumeByKey(from.getUuid());
            if (request == null) {
                denier.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.error.no_request_from_player", from.getName().getString()));
                return 0;
            }
        }

        Pair<String, String> deniedMessages = request.getDeniedKeys();
        if (deniedMessages.first() != null)
            denier.sendMessage(MCTextUtils.fromLang(deniedMessages.second(), from.getName().getString()));
        if (deniedMessages.second() != null)
            from.sendMessage(MCTextUtils.fromLang(deniedMessages.first(), denier.getName().getString()));
        return 1;
    }

    public void doTeleport(
        ServerPlayerEntity player,
        ServerWorld world,
        BlockPos position
    ) {
        world.getServer().execute(() -> {
            // this is version sensitive!
            player.teleport(
                world,
                position.getX(), position.getY(), position.getZ(),
                PositionFlag.DELTA,
                player.getYaw(), player.getPitch(),
                false
            );
        });
    }

    public PlayerData getPlayerData(UUID key) {
        return playerDatas.computeIfAbsent(key, k -> new PlayerData());
    }

    public ObjectCollection<PlayerData> getPlayerDatas() {
        return playerDatas.values();
    }
}
