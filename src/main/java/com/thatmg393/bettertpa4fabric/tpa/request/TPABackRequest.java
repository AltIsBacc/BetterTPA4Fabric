package com.thatmg393.bettertpa4fabric.tpa.request;

import java.util.function.Consumer;

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
        Consumer<TeleportTask.Result> callback = buildCallback(getRequester(), getTarget());
        return new TeleportTask(
            getRequester(), getTarget().getLeft(),
            BetterTPA4Fabric.CONFIG.tpaTeleportTime * 20,
            res -> {
                callback.accept(res);
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
