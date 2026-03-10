package com.thatmg393.bettertpa4fabric.tpa.tickable.task;

import com.thatmg393.bettertpa4fabric.tpa.TeleportManager;
import com.thatmg393.bettertpa4fabric.tpa.data.PlayerData;
import com.thatmg393.bettertpa4fabric.tpa.tickable.task.base.TickableTask;
import com.thatmg393.bettertpa4fabric.utils.MCTextUtils;

import it.unimi.dsi.fastutil.Pair;

public class StaleRequestsCleanerTask extends TickableTask {

    public StaleRequestsCleanerTask() {
        super(20);
    }

    @Override
    protected TickResult onTick() {
        return TickResult.REPEAT;
    }

    @Override
    protected void onFinish() {
        for (PlayerData data : TeleportManager.INSTANCE.getPlayerDatas()) {
            if (data.teleportRequests.isEmpty()) continue;
            data.teleportRequests.values().removeIf(request -> {
                if (!request.isExpired()) return false;

                Pair<String, String> expiredMessages = request.getExpiredKeys();
                if (expiredMessages.first() != null && request.getRequester().networkHandler.isConnectionOpen()) {
                    request.getRequester().sendMessage(MCTextUtils.fromLang(
                        expiredMessages.first(),
                        request.getTarget().getLeft().map(p -> p.getName().getString()).orElse("?")
                    ));
                }

                request.getTarget().ifLeft(t -> {
                    if (expiredMessages.second() != null && t.networkHandler.isConnectionOpen()) {
                        t.sendMessage(MCTextUtils.fromLang(
                            expiredMessages.second(),
                            request.getRequester().getName().getString()
                        ));
                    }
                });

                return true;
            });
        }
    }
}
