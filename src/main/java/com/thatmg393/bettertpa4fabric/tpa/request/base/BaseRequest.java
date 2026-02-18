package com.thatmg393.bettertpa4fabric.tpa.request.base;

import static com.thatmg393.bettertpa4fabric.utils.MCTextUtils.fromLang;

import java.util.Timer;
import java.util.TimerTask;

import com.thatmg393.bettertpa4fabric.BetterTPA4Fabric;
import com.thatmg393.bettertpa4fabric.config.ModConfigManager;
import com.thatmg393.bettertpa4fabric.tpa.wrapper.TPAPlayerWrapper;

public abstract class BaseRequest {
    private final Timer expirationTimer = new Timer();

    public final TPAPlayerWrapper requester;
    public final TPAPlayerWrapper receiver;

    public BaseRequest(TPAPlayerWrapper requester, TPAPlayerWrapper receiver) {
        this.requester = requester;
        this.receiver = receiver;

         expirationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                receiver.removeTPARequest(requester.uuid);

                requester.sendMessage(fromLang("bettertpa4fabric.message.requester.tpa.expire", receiver.name));
                receiver.sendMessage(fromLang("bettertpa4fabric.message.receiver.tpa.expire", requester.name));
            }
        }, ModConfigManager.loadOrGetConfig().tpaExpireTime * 1000);
    }

    private void consume() {
        expirationTimer.cancel();
        BetterTPA4Fabric.LOGGER.info("Consumed TPA request from " + requester.name);
    }

    public void accept() {
        consume();
    }

    public void deny() {
        consume();
    }

    public void default_onTick(long delta) {
        if ((delta % 1000) == 0) {
            float remain = (delta / 1000);
            requester.sendMessage(fromLang("bettertpa4fabric.message.teleport.countdown", remain));
            receiver.sendMessage(fromLang("bettertpa4fabric.message.teleport.countdown", remain));
        }
    }
}
