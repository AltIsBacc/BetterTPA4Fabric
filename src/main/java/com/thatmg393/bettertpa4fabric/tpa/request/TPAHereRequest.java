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
        
        TeleportManager.INSTANCE.getPlayerData(teleportingPlayer.getUuid()).isPlayerTeleporting = true;
        
        return new TeleportTask(
            teleportingPlayer, Optional.of(getRequester()),
            BetterTPA4Fabric.CONFIG.tpaTeleportTime * 20,
            buildCallback(teleportingPlayer, Either.left(getRequester()))
        );
    }

    @Override
    public Pair<String, String> getExpiredKeys() {
        return Pair.of(
            "bettertpa4fabric.message.tpahere.expired.requester",
            "bettertpa4fabric.message.tpahere.expired.receiver"
        );
    }
}
