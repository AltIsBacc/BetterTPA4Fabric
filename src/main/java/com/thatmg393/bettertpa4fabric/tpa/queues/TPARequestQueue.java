package com.thatmg393.bettertpa4fabric.tpa.queues;

public class TPARequestQueue {
    private final Object2ObjectLinkedOpenHashMap<UUID, TPARequest> bySender = new Object2ObjectLinkedOpenHashMap<>();

    void add(TPARequest r) {
        bySender.put(r.sender, r);
    }

    void remove(UUID sender) {
        bySender.remove(sender);
    }

    boolean isEmpty() {
        return bySender.isEmpty();
    }

    int size() {
        return bySender.size();
    }

    TPARequest getLatest() {
        if (bySender.isEmpty())
            return null;
        TPARequest r = bySender.get(bySender.lastKey());
        return r.isExpired() ? null : r;
    }

    TPARequest getBySender(UUID sender) {
        TPARequest r = bySender.get(sender);
        return (r != null && !r.isExpired()) ? r : null;
    }

    void purgeExpired() {
        bySender.values().removeIf(TPARequest::isExpired);
    }
}
