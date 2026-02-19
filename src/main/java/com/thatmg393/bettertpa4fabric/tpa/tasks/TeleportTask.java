package com.thatmg393.bettertpa4fabric.tpa.tasks;

import com.thatmg393.bettertpa4fabric.tpa.tasks.result.TaskResult;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class TeleportTask {
    private final ServerPlayerEntity player;
    private final ServerPlayerEntity dest;
    private final BlockPos startPos;
    private final Runnable onSuccess;

    private boolean cancelled;
    private int ticksRemaining;

    public TeleportTask(ServerPlayerEntity player, ServerPlayerEntity dest, int taskTicks, Runnable onSuccess) {
        this.player = player;
        this.dest = dest;
        this.startPos = player.getBlockPos();
        this.onSuccess = onSuccess;
        this.ticksRemaining = taskTicks;
    }

    public boolean tick() {
        if (cancelled) {
            onTaskResult(TaskResult.CANCELLED);
            return false;
        }

        if (dest != null && (!dest.isAlive() || dest.isDead())) {
            cancelled = true;
            onTaskResult(TaskResult.DESTINATION_GONE);
        }

        if (!player.getBlockPos().equals(startPos)) {
            cancelled = true;
            onTaskResult(TaskResult.MOVED);
            return false;
        }

        if (--ticksRemaining <= 0) {
            onTaskResult(TaskResult.SUCCESS);
            return false;
        }

        // notify player: ticksRemaining / 20 + "s remaining..." (every 20 ticks)
        return true;
    }

    public void cancel() {
        cancelled = true;
    }

    private void onTaskResult(TaskResult result) {
        switch (result) {
            case SUCCESS:
                onSuccess.run();
                break;

            case CANCELLED:
                // notify player: "Teleport cancelled"
                // notify dest: "Teleport cancelled"
                break;

            case MOVED:
                // notify player: "You moved! Teleport cancelled"
                // notify dest: "<player> moved, teleport cancelled"
                break;

            case DESTINATION_GONE:
                // notify player: "Destination disappeared! Teleport cancelled"
                break;
        }
    }
}
