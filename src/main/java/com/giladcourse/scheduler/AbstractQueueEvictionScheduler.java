package com.giladcourse.scheduler;

import com.giladcourse.EvictionQueue;
import com.giladcourse.EvictionScheduler;
import com.giladcourse.map.EvictibleEntry;
import com.giladcourse.queue.NavigableMapEvictionQueue;

public abstract class AbstractQueueEvictionScheduler<K, V> implements EvictionScheduler<K, V> {

    private final EvictionQueue<K, V> queue;

    public AbstractQueueEvictionScheduler() {
        this(new NavigableMapEvictionQueue<K, V>());
    }

    public AbstractQueueEvictionScheduler(EvictionQueue<K, V> queue) {
        super();
        if (queue == null) {
            throw new NullPointerException("Queue cannot be null");
        }

        this.queue = queue;
    }

    @Override
    public void scheduleEviction(EvictibleEntry<K, V> e) {
        if (e.isEvictible()) {
            queue.putEntry(e);
            onScheduleEviction(e);
        }
    }

    @Override
    public void cancelEviction(EvictibleEntry<K, V> e) {
        if (e.isEvictible()) {
            queue.removeEntry(e);
            onCancelEviction(e);
        }
    }

    protected void evictEntries() {
        if (queue.evictEntries()) {
            onEvictEntries();
        }
    }

    protected boolean hasScheduledEvictions() {
        return queue.hasEntries();
    }

    protected long getNextEvictionTime() {
        return queue.getNextEvictionTime();
    }

    protected abstract void onScheduleEviction(EvictibleEntry<K, V> e);

    protected abstract void onCancelEviction(EvictibleEntry<K, V> e);

    protected abstract void onEvictEntries();

    final class EvictionRunnable implements Runnable {
        @Override
        public void run() {
            evictEntries();
        }
    }
}
