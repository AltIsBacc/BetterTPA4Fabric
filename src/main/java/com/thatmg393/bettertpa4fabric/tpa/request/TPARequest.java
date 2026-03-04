package com.thatmg393.bettertpa4fabric.tpa.request;

import com.thatmg393.bettertpa4fabric.BetterTPA4Fabric;
import com.thatmg393.bettertpa4fabric.tpa.TeleportManager;
import com.thatmg393.bettertpa4fabric.tpa.request.base.BaseRequest;
import com.thatmg393.bettertpa4fabric.tpa.tickable.task.TeleportTask;
import com.thatmg393.bettertpa4fabric.utils.Either;

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
}
