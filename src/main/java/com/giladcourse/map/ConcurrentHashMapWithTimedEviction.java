package com.giladcourse.map;

import java.util.concurrent.ConcurrentHashMap;

import com.giladcourse.scheduler.DelayedTaskEvictionScheduler;
import com.giladcourse.ConcurrentMapWithTimedEviction;
import com.giladcourse.EvictionScheduler;


public class ConcurrentHashMapWithTimedEviction<K, V> extends ConcurrentMapWithTimedEvictionDecorator<K, V> implements ConcurrentMapWithTimedEviction<K, V> {

    public ConcurrentHashMapWithTimedEviction(int initialCapacity, float loadFactor, int concurrencyLevel, EvictionScheduler<K, V> scheduler) {
        super(new ConcurrentHashMap<K, EvictibleEntry<K, V>>(initialCapacity, loadFactor, concurrencyLevel), scheduler);
    }


    public ConcurrentHashMapWithTimedEviction(int initialCapacity, float loadFactor, int concurrencyLevel) {
        this(initialCapacity, loadFactor, concurrencyLevel, ConcurrentHashMapWithTimedEviction.<K, V> defaultScheduler());
    }


    public ConcurrentHashMapWithTimedEviction(int initialCapacity, float loadFactor, EvictionScheduler<K, V> scheduler) {
        super(new ConcurrentHashMap<K, EvictibleEntry<K, V>>(initialCapacity, loadFactor), scheduler);
    }


    public ConcurrentHashMapWithTimedEviction(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, ConcurrentHashMapWithTimedEviction.<K, V> defaultScheduler());
    }

    public ConcurrentHashMapWithTimedEviction(int initialCapacity, EvictionScheduler<K, V> scheduler) {
        super(new ConcurrentHashMap<K, EvictibleEntry<K, V>>(initialCapacity), scheduler);
    }


    public ConcurrentHashMapWithTimedEviction(int initialCapacity) {
        this(initialCapacity, ConcurrentHashMapWithTimedEviction.<K, V> defaultScheduler());
    }

    public ConcurrentHashMapWithTimedEviction(EvictionScheduler<K, V> scheduler) {
        super(new ConcurrentHashMap<K, EvictibleEntry<K, V>>(), scheduler);
    }

    public ConcurrentHashMapWithTimedEviction() {
        this(ConcurrentHashMapWithTimedEviction.<K, V> defaultScheduler());
    }

    private static <K, V> EvictionScheduler<K, V> defaultScheduler() {
        return new DelayedTaskEvictionScheduler<K, V>();
    }
}
