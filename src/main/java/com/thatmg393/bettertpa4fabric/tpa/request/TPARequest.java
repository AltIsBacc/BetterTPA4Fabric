package com.thatmg393.bettertpa4fabric.tpa.request;

import com.thatmg393.bettertpa4fabric.BetterTPA4Fabric;
import com.thatmg393.bettertpa4fabric.tpa.TeleportManager;
import com.thatmg393.bettertpa4fabric.tpa.request.base.BaseRequest;
import com.thatmg393.bettertpa4fabric.tpa.tickable.task.TeleportTask;
import com.thatmg393.bettertpa4fabric.utils.Either;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.server.network.ServerPlayerEntity;

public class TPARequest extends BaseRequest {
    public TPARequest(ServerPlayerEntity requester, ServerPlayerEntity target) {
        super(requester, Either.left(target));
    }

    @Override
    public TeleportTask accept() {
        TeleportManager.INSTANCE.getPlayerData(getRequester().getUuid()).isPlayerTeleporting = true;
        return new TeleportTask(
            getRequester(), getTarget().getLeft(),
            BetterTPA4Fabric.CONFIG.tpaTeleportTime * 20,
            buildCallback(getRequester(), getTarget())
        );
    }

    @Override
    public Pair<String, String> getExpiredKeys() {
        return Pair.of(
            "bettertpa4fabric.message.tpa.expired.sender",
            "bettertpa4fabric.message.tpa.expired.accepter"
        );
    }

    @Override
    public Pair<String, String> getAcceptedKeys() {
        return Pair.of(
            "bettertpa4fabric.message.tpa.accepted.sender",
            "bettertpa4fabric.message.tpa.accepted.accepter"
        );
    }

    @Override
    public Pair<String, String> getDeniedKeys() {
        return Pair.of(
            "bettertpa4fabric.message.tpa.denied.sender",
            "bettertpa4fabric.message.tpa.denied.denier"
        );
    }
}
