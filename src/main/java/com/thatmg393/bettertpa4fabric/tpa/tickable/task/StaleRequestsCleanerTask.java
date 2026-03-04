package com.thatmg393.bettertpa4fabric.tpa.tickable.task;

import com.thatmg393.bettertpa4fabric.tpa.TeleportManager;
import com.thatmg393.bettertpa4fabric.tpa.tickable.task.base.TickableTask;
import com.thatmg393.bettertpa4fabric.utils.MCTextUtils;

import it.unimi.dsi.fastutil.Pair;

public class StaleRequestsCleanerTask extends TickableTask {

    public StaleRequestsCleanerTask() {
        super(5);
    }

    @Override
    protected TickResult onTick() {
        if ((getTickDuration() % 20) == 0) {
            TeleportManager.INSTANCE.streamPlayerDatas()
                .map(e -> e.getValue().teleportRequests)
                .forEach(queue -> {
                    queue.values().removeIf(request -> {
                        if (!request.isExpired()) return false;

                        Pair<String, String> expiredMessages = request.getExpiredKeys();
                        request.getRequester().sendMessage(MCTextUtils.fromLang(expiredMessages.first(),
                            request.getTarget().getLeft().map(p -> p.getName().getString()).orElse("?")
                        ));

                        request.getTarget().ifLeft(
                            t -> t.sendMessage(MCTextUtils.fromLang(expiredMessages.second(), request.getRequester().getName().getString())
                        ));

                        return true;
                    });
                });
        }

        return TickResult.RESET;
    }

    @Override
    protected void onFinish() {
        throw new IllegalStateException("onFinish should never be called inside StaleRequestsCleanerTask");
    }
}
