package com.thatmg393.bettertpa4fabric.tpa;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.thatmg393.bettertpa4fabric.tpa.data.PlayerData;
import com.thatmg393.bettertpa4fabric.tpa.request.TPABackRequest;
import com.thatmg393.bettertpa4fabric.tpa.request.TPAHereRequest;
import com.thatmg393.bettertpa4fabric.tpa.request.TPARequest;
import com.thatmg393.bettertpa4fabric.tpa.request.base.BaseRequest;
import com.thatmg393.bettertpa4fabric.tpa.tickable.TickableTaskProcessor;
import com.thatmg393.bettertpa4fabric.tpa.tickable.task.TeleportTask;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class TeleportManager {
    public static final TeleportManager INSTANCE = new TeleportManager();

    private final Object2ObjectOpenHashMap<UUID, PlayerData> playerDatas = new Object2ObjectOpenHashMap<>();
    private final TickableTaskProcessor<TeleportTask> teleportTasks = new TickableTaskProcessor<>();

    public void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> doTick());
        ServerPlayerEvents.LEAVE.register(player -> playerDatas.remove(player.getUuid()));
    }

    public int teleportTo(ServerPlayerEntity requester, ServerPlayerEntity target) {
        PlayerData targetData = getPlayerData(target.getUuid());
        targetData.teleportRequests.add(
            requester.getUuid(),
            new TPARequest(requester, target)
        );

        return 1;
    }

    public int teleportHere(ServerPlayerEntity requester, ServerPlayerEntity target) {
        PlayerData targetData = getPlayerData(target.getUuid());
        targetData.teleportRequests.add(
            requester.getUuid(),
            new TPAHereRequest(requester, target)
        );

        return 1;
    }

    public int teleportBack(ServerPlayerEntity requester) {
        PlayerData requesterData = getPlayerData(requester.getUuid());

        if (requesterData.previousTeleportPosition == null) {
            // requester: no previous position to teleport to
            return 0;
        }

        teleportTasks.putTask(
            new TPABackRequest(requester, requesterData.previousTeleportPosition).accept()
        );

        return 1;
    }

    public int acceptTeleport(ServerPlayerEntity requester, @Nullable ServerPlayerEntity target) {
        PlayerData requesterData = getPlayerData(requester.getUuid());

        if (target == null) {
            BaseRequest request = requesterData.teleportRequests.consume();
            if (request == null) {
                // requester: no received teleport request
                return 0;
            }

            teleportTasks.putTask(request.accept());
        } else {
            if (requesterData.teleportRequests.containsKey(target.getUuid())) {
                teleportTasks.putTask(
                    requesterData.teleportRequests.consumeByKey(
                        target.getUuid()
                    ).accept()
                );
            } else {
                // requester: no teleport request from target
                return 0;
            }
        }

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


    public void doTick() {
        teleportTasks.doTick();
    }

    public PlayerData getPlayerData(UUID key) {
        return playerDatas.computeIfAbsent(key, k -> new PlayerData());
    }
}
