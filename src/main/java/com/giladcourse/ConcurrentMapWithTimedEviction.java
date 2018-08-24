package com.giladcourse;

import java.util.concurrent.ConcurrentMap;

public interface ConcurrentMapWithTimedEviction<K, V> extends ConcurrentMap<K, V> {

    V put(K key, V value, long evictMs);

    V putIfAbsent(K key, V value, long evictMs);

    V replace(K key, V value, long evictMs);

    boolean replace(K key, V oldValue, V newValue, long evictMs);
}
