package com.giladcourse;

import com.giladcourse.map.EvictibleEntry;


public interface EvictionScheduler<K, V> {


    void scheduleEviction(EvictibleEntry<K, V> e);

    void cancelEviction(EvictibleEntry<K, V> e);

    void shutdown();
}
