package com.thatmg393.bettertpa4fabric.tpa.request;

import com.thatmg393.bettertpa4fabric.BetterTPA4Fabric;
import com.thatmg393.bettertpa4fabric.tpa.TeleportManager;
import com.thatmg393.bettertpa4fabric.tpa.request.base.BaseRequest;
import com.thatmg393.bettertpa4fabric.tpa.tickable.task.TeleportTask;
import com.thatmg393.bettertpa4fabric.utils.Either;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TPABackRequest extends BaseRequest {
    public TPABackRequest(ServerPlayerEntity requester, Pair<RegistryKey<World>, BlockPos> target) {
        super(requester, Either.right(target));
    }

    @Override
    public TeleportTask accept() {
        TeleportManager.INSTANCE.getPlayerData(getRequester().getUuid()).isPlayerTeleporting = true;
        
        return new TeleportTask(
            getRequester(), getTarget().getLeft(),
            BetterTPA4Fabric.CONFIG.tpaTeleportTime * 20,
            res -> {
                buildCallback(getRequester(), getTarget()).accept(res);
                if (res == TeleportTask.Result.SUCCESS && BetterTPA4Fabric.CONFIG.oneTimeTPABack) {
                    TeleportManager.INSTANCE.getPlayerData(getRequester().getUuid()).previousTeleportPosition = null;
                }
            }
        );
    }

    @Override
    public boolean isExpired() {
        return false;
    }
}
