package com.thatmg393.bettertpa4fabric.tpa.request;

import com.thatmg393.bettertpa4fabric.BetterTPA4Fabric;
import com.thatmg393.bettertpa4fabric.tpa.TeleportManager;
import com.thatmg393.bettertpa4fabric.tpa.request.base.BaseRequest;
import com.thatmg393.bettertpa4fabric.tpa.tickable.task.TeleportTask;
import com.thatmg393.bettertpa4fabric.utils.Either;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class TPABackRequest extends BaseRequest {
    public TPABackRequest(ServerPlayerEntity requester, Pair<ServerWorld, BlockPos> target) {
        super(requester, Either.right(target));
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
