package com.giladcourse;

import com.giladcourse.map.EvictibleEntry;
import com.giladcourse.scheduler.AbstractQueueEvictionScheduler;

public interface EvictionQueue<K, V> {

    boolean hasEntries();
    long getNextEvictionTime();
    void putEntry(EvictibleEntry<K, V> e);
    void removeEntry(EvictibleEntry<K, V> e);
    boolean evictEntries();
}
