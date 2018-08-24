package com.giladcourse.map;

import java.util.Map.Entry;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class EvictibleEntry<K, V> implements Entry<K, V> {

    private final ConcurrentMapWithTimedEvictionDecorator<K, V> map;

    private final K key;

    private volatile V value;

    private final long evictMs;

    private final boolean evictible;

    private final long evictionTime;

    private volatile Object data;


    EvictibleEntry(ConcurrentMapWithTimedEvictionDecorator<K, V> map, K key, V value, long evictMs) {
        if (value == null) {
            throw new NullPointerException("Value cannot be null");
        }

        if (evictMs < 0) {
            throw new IllegalArgumentException("Eviction time cannot be less than zero");
        }

        this.map = map;
        this.key = key;
        this.value = value;
        this.evictMs = evictMs;
        this.evictible = (evictMs > 0);
        this.evictionTime = (evictible) ? System.nanoTime() + NANOSECONDS.convert(evictMs, MILLISECONDS) : 0;
    }

    @Override
    public K getKey() {
        return this.key;
    }

    @Override
    public V getValue() {
        return this.value;
    }


    @Override
    public synchronized V setValue(V value) {
        if (value == null) {
            throw new NullPointerException("value cannot be null");
        }

        V oldValue = this.value;
        this.value = value;
        return oldValue;
    }

    public boolean isEvictible() {
        return this.evictible;
    }

    public long getEvictionTime() {
        return this.evictionTime;
    }


    public Object getData() {
        return this.data;
    }

    public void setData(Object data) {
        this.data = data;
    }


    public boolean shouldEvict() {
        return (this.evictible) && (System.nanoTime() > this.evictionTime);
    }


    public void evict(boolean cancelPendingEviction) {
        this.map.evict(this, cancelPendingEviction);
    }

    @Override
    public String toString() {
        return String.format("[%s, %s, %d]", (key != null) ? key : "null", value, evictMs);
    }

}
