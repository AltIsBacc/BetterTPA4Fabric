package com.thatmg393.bettertpa4fabric.tpa.tickable.task;

import java.util.Optional;
import java.util.function.Consumer;

import com.thatmg393.bettertpa4fabric.BetterTPA4Fabric;
import com.thatmg393.bettertpa4fabric.tpa.tickable.task.base.TickableTask;
import com.thatmg393.bettertpa4fabric.utils.MCTextUtils;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class TeleportTask extends TickableTask {

    public enum Result {
        REQUESTER_DIED,
        TARGET_DIED,
        REQUESTER_MOVED,
        SUCCESS
    }

    private final ServerPlayerEntity requester;
    private final Optional<ServerPlayerEntity> target;
    private final Consumer<Result> callback;

    private BlockPos startPos;

    public TeleportTask(
        ServerPlayerEntity requester,
        Optional<ServerPlayerEntity> target,
        long tickDuration,
        Consumer<Result> callback
    ) {
        super(tickDuration);

        this.requester = requester;
        this.target = target;
        this.callback = callback;
    }

    @Override
    protected void onFirstTick() {
        startPos = requester.getBlockPos();
    }

    @Override
    protected TickResult onTick() {
        if (!requester.isAlive()) {
            callback.accept(Result.REQUESTER_DIED);
            return TickResult.CANCEL;
        }

        if (target.isPresent() && !target.get().isAlive()) {
            callback.accept(Result.TARGET_DIED);
            return TickResult.CANCEL;
        }

        if (!(
            requester.getBlockX() == startPos.getX() &&
            requester.getBlockZ() == startPos.getZ()
        )) {
            callback.accept(Result.REQUESTER_MOVED);
            return BetterTPA4Fabric.CONFIG.resetTimerOnMove ? TickResult.RESET : TickResult.CANCEL;
        }

        if (getTickDuration() % 20 == 0) {
            requester.sendMessage(MCTextUtils.fromLang("bettertpa4fabric.message.teleport.countdown", getTickDuration() / 20));
        }

        return TickResult.CONTINUE;
    }

    @Override
    protected void onFinish() {
        callback.accept(Result.SUCCESS);
    }
}
