package com.thatmg393.bettertpa4fabric.tpa.request.base;

import java.util.function.Consumer;

import com.thatmg393.bettertpa4fabric.BetterTPA4Fabric;
import com.thatmg393.bettertpa4fabric.tpa.TeleportManager;
import com.thatmg393.bettertpa4fabric.tpa.tickable.task.TeleportTask;
import com.thatmg393.bettertpa4fabric.utils.Either;
import com.thatmg393.bettertpa4fabric.utils.MCTextUtils;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class BaseRequest {
    public static Consumer<TeleportTask.Result> buildCallback(
        ServerPlayerEntity teleportingPlayer,
        Either<ServerPlayerEntity, Pair<RegistryKey<World>, BlockPos>> target
    ) {
        TeleportManager.INSTANCE.getPlayerData(teleportingPlayer.getUuid()).isTeleportingLocked = true;
        target.ifLeft(t -> TeleportManager.INSTANCE.getPlayerData(t.getUuid()).isTeleportingLocked = true);

        return res -> {
            TeleportManager.INSTANCE.getPlayerData(teleportingPlayer.getUuid()).isTeleportingLocked = false;
            target.ifLeft(t -> TeleportManager.INSTANCE.getPlayerData(t.getUuid()).isTeleportingLocked = false);

            switch (res) {
                case REQUESTER_MOVED -> {
                    String key1 = "bettertpa4fabric.message.error.cancelled.you_moved";
                    String key2 = "bettertpa4fabric.message.error.cancelled.they_moved";

                    if (BetterTPA4Fabric.CONFIG.resetTimerOnMove) {
                        TeleportManager.INSTANCE.getPlayerData(teleportingPlayer.getUuid()).isTeleportingLocked = true;
                        target.ifLeft(t -> TeleportManager.INSTANCE.getPlayerData(t.getUuid()).isTeleportingLocked = true);

                        key1 = "bettertpa4fabric.message.error.reset.you_moved";
                        key2 = "bettertpa4fabric.message.error.reset.they_moved";
                    }

                    teleportingPlayer.sendMessage(MCTextUtils.fromLang(key1));

                    final String realKey2 = key2;
                    target.ifLeft(t -> t.sendMessage(MCTextUtils.fromLang(realKey2)));
                }
                case REQUESTER_DIED -> {
                    teleportingPlayer.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.error.cancelled.you_died"));
                    target.ifLeft(t -> t.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.error.cancelled.they_died")));
                }
                case TARGET_DIED -> {
                    teleportingPlayer.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.error.cancelled.target_died"));
                    target.ifLeft(t -> t.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.error.cancelled.target_died.notify")));
                }
                case SUCCESS -> {
                    target.ifLeft(t -> TeleportManager.INSTANCE.doTeleport(teleportingPlayer, t.getEntityWorld(), t.getBlockPos()))
                        .ifRight(pos -> TeleportManager.INSTANCE.doTeleport(teleportingPlayer, pos.first(), pos.second()));
                }
            }
        };
    }

    private final ServerPlayerEntity requester;
    private final Either<ServerPlayerEntity, Pair<RegistryKey<World>, BlockPos>> target;

    private final long createdAt = System.currentTimeMillis();

    public BaseRequest(ServerPlayerEntity requester, Either<ServerPlayerEntity, Pair<RegistryKey<World>, BlockPos>> target) {
        this.requester = requester;
        this.target = target;
    }

    public ServerPlayerEntity getRequester() {
        return requester;
    }

    public Either<ServerPlayerEntity, Pair<RegistryKey<World>, BlockPos>> getTarget() {
        return target;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > createdAt + (BetterTPA4Fabric.CONFIG.tpaExpireTime * 1000);
    }

    public Pair<String, String> getExpiredKeys() {
        return Pair.of(null, null);
    }

    public Pair<String, String> getAcceptedKeys() {
        return Pair.of(null, null);
    }

    public Pair<String, String> getDeniedKeys() {
        return Pair.of(null, null);
    }

    public abstract TeleportTask accept();
}
