package com.thatmg393.bettertpa4fabric.tpa.request;

import java.util.Optional;

import com.thatmg393.bettertpa4fabric.BetterTPA4Fabric;
import com.thatmg393.bettertpa4fabric.tpa.TeleportManager;
import com.thatmg393.bettertpa4fabric.tpa.request.base.BaseRequest;
import com.thatmg393.bettertpa4fabric.tpa.tickable.task.TeleportTask;
import com.thatmg393.bettertpa4fabric.utils.Either;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.server.network.ServerPlayerEntity;

public class TPAHereRequest extends BaseRequest {
    public TPAHereRequest(ServerPlayerEntity requester, ServerPlayerEntity target) {
        super(requester, Either.left(target));
    }

    @Override
    public TeleportTask accept() {
        ServerPlayerEntity teleportingPlayer = getTarget().getLeft().get();
        
        TeleportManager.INSTANCE.getPlayerData(getRequester().getUuid()).isPlayerTeleporting = true;
        TeleportManager.INSTANCE.getPlayerData(teleportingPlayer.getUuid()).isPlayerTeleporting = true;

        return new TeleportTask(
            teleportingPlayer, Optional.of(getRequester()),
            BetterTPA4Fabric.CONFIG.tpaTeleportTime * 20,
            res -> {
                TeleportManager.INSTANCE.getPlayerData(getRequester().getUuid()).isPlayerTeleporting = (res == TeleportTask.Result.REQUESTER_MOVED && BetterTPA4Fabric.CONFIG.resetTimerOnMove);
                buildCallback(teleportingPlayer, Either.left(getRequester())).accept(res);
            }
        );
    }

    @Override
    public Pair<String, String> getExpiredKeys() {
        return Pair.of(
            "bettertpa4fabric.message.tpahere.expired.sender",
            "bettertpa4fabric.message.tpahere.expired.accepter"
        );
    }

    @Override
    public Pair<String, String> getAcceptedKeys() {
        return Pair.of(
            "bettertpa4fabric.message.tpahere.accepted.sender",
            "bettertpa4fabric.message.tpahere.accepted.accepter"
        );
    }

    @Override
    public Pair<String, String> getDeniedKeys() {
        return Pair.of(
            "bettertpa4fabric.message.tpahere.denied.sender",
            "bettertpa4fabric.message.tpahere.denied.denier"
        );
    }
}
