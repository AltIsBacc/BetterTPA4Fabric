package com.thatmg393.bettertpa4fabric.tpa;

import java.util.UUID;

public class TPARequest {
    public enum Type { TPA, TPA_HERE }

    public final UUID sender;
    public final UUID target;
    public final Type type;
    public final long expiresAt;

    public TPARequest(UUID sender, UUID target, Type type, long timeoutMs) {
        this.sender = sender;
        this.target = target;
        this.type = type;
        this.expiresAt = System.currentTimeMillis() + timeoutMs;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
