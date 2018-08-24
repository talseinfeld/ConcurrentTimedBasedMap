package com.giladcourse.queue;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import com.giladcourse.map.EvictibleEntry;
import com.giladcourse.EvictionQueue;

public class NavigableMapEvictionQueue<K, V> implements EvictionQueue<K, V> {

    private final ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> map;

    public NavigableMapEvictionQueue() {
        this(new ConcurrentSkipListMap<Long, EvictibleEntry<K, V>>());
    }

    public NavigableMapEvictionQueue(ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> map) {
        if (map == null) {
            throw new NullPointerException("Map instnace cannot be null");
        }

        this.map = map;
    }

    @Override
    public boolean hasEntries() {
        return !map.isEmpty();
    }

    @Override
    public long getNextEvictionTime() {
        try {
            return (!map.isEmpty()) ? map.firstKey() : 0;
        } catch (NoSuchElementException e) {
            return 0;
        }
    }

    @Override
    public void putEntry(EvictibleEntry<K, V> e) {
        map.put(e.getEvictionTime(), e);
    }

    @Override
    public void removeEntry(EvictibleEntry<K, V> e) {
        map.remove(e.getEvictionTime(), e);
    }

    @Override
    public boolean evictEntries() {
        boolean result = false;
        ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> head = map.headMap(System.nanoTime());
        if (!head.isEmpty()) {
            for (EvictibleEntry<K, V> e : head.values()) {
                e.evict(false);
            }
            head.clear();
            result = true;
        }
        return result;
    }

}
