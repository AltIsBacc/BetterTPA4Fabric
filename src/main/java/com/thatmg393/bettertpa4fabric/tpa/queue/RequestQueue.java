package com.thatmg393.bettertpa4fabric.tpa.queue;

import com.thatmg393.bettertpa4fabric.tpa.request.base.BaseRequest;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

public class RequestQueue<K, V extends BaseRequest> extends Object2ObjectLinkedOpenHashMap<K, V> {
    
    public void add(K key, V value) {
        putAndMoveToLast(key, value);
    }

    public void insertInFront(K key, V value) {
        putAndMoveToFirst(key, value);
    }

    // Consume from front
    public V consume() {
        if (isEmpty()) return null;
        return removeFirst();
    }

    // Consume from back
    public V popBack() {
        if (isEmpty()) return null;
        return removeLast();
    }

    // Find by key WITHOUT consuming
    public V findByKey(K key) {
        return get(key);
    }

    // Consume by key
    public V consumeByKey(K key) {
        return remove(key);
    }
}
