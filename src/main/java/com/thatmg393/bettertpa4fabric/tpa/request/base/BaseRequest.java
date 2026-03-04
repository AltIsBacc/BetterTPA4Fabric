package com.thatmg393.bettertpa4fabric.tpa.request.base;

import java.util.function.Consumer;

import com.thatmg393.bettertpa4fabric.BetterTPA4Fabric;
import com.thatmg393.bettertpa4fabric.tpa.TeleportManager;
import com.thatmg393.bettertpa4fabric.tpa.tickable.task.TeleportTask;
import com.thatmg393.bettertpa4fabric.utils.Either;
import com.thatmg393.bettertpa4fabric.utils.MCTextUtils;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public abstract class BaseRequest {
    public static Consumer<TeleportTask.Result> buildCallback(
        ServerPlayerEntity teleportingPlayer,
        Either<ServerPlayerEntity, Pair<ServerWorld, BlockPos>> target
    ) {
        return res -> {
            TeleportManager.INSTANCE.getPlayerData(teleportingPlayer.getUuid()).isPlayerTeleporting = false;
            switch (res) {
                case REQUESTER_MOVED -> {
                    teleportingPlayer.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.error.cancelled.requester_moved"));
                    target.ifLeft(t -> t.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.error.cancelled.requester_moved.receiver")));
                }
                case REQUESTER_DIED -> {
                    teleportingPlayer.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.error.cancelled.requester_dead"));
                    target.ifLeft(t -> t.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.error.cancelled.requester_dead.receiver")));
                }
                case TARGET_DIED -> {
                    teleportingPlayer.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.error.cancelled.target_dead.requester"));
                    target.ifLeft(t -> t.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.error.cancelled.target_dead")));
                }
                case SUCCESS -> {
                    teleportingPlayer.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.teleport.success"));
                    TeleportManager.INSTANCE.getPlayerData(teleportingPlayer.getUuid()).previousTeleportPosition = Pair.of(teleportingPlayer.getEntityWorld(), teleportingPlayer.getBlockPos());

                    target.ifLeft(t -> TeleportManager.INSTANCE.doTeleport(teleportingPlayer, t.getEntityWorld(), t.getBlockPos()))
                        .ifRight(pos -> TeleportManager.INSTANCE.doTeleport(teleportingPlayer, pos.first(), pos.second()));
                }
            }
        };
    }

    private final ServerPlayerEntity requester;
    private final Either<ServerPlayerEntity, Pair<ServerWorld, BlockPos>> target;

    private final long createdAt = System.currentTimeMillis();

    public BaseRequest(ServerPlayerEntity requester, Either<ServerPlayerEntity, Pair<ServerWorld, BlockPos>> target) {
        this.requester = requester;
        this.target = target;
    }

    public ServerPlayerEntity getRequester() {
        return requester;
    }

    public Either<ServerPlayerEntity, Pair<ServerWorld, BlockPos>> getTarget() {
        return target;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > createdAt + (BetterTPA4Fabric.CONFIG.tpaExpireTime * 1000);
    }

    public abstract TeleportTask accept();
}
