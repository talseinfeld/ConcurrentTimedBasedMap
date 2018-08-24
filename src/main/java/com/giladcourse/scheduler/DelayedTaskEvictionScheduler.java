package com.giladcourse.scheduler;


import com.giladcourse.EvictionQueue;
import com.giladcourse.map.EvictibleEntry;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;


public class DelayedTaskEvictionScheduler<K, V> extends AbstractQueueEvictionScheduler<K, V> {

    public static final int DEFAULT_THREAD_POOL_SIZE = 1;

    private final ScheduledExecutorService ses;

    private volatile ScheduledFuture<?> future = null;

    private volatile long next = 0;

    public DelayedTaskEvictionScheduler() {
        this(new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE));
    }


    public DelayedTaskEvictionScheduler(ScheduledExecutorService ses) {
        super();

        if (ses == null) {
            throw new NullPointerException("ScheduledExecutorService instance cannot be null");
        }

        this.ses = ses;
    }


    public DelayedTaskEvictionScheduler(EvictionQueue<K, V> queue) {
        this(queue, new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE));
    }


    public DelayedTaskEvictionScheduler(EvictionQueue<K, V> queue, ScheduledExecutorService executorService) {
        super(queue);
        if (executorService == null) {
            throw new NullPointerException("ScheduledExecutorService instance cannot be null");
        }

        this.ses = executorService;
    }

    @Override
    public void shutdown() {
        ses.shutdownNow();
    }


    @Override
    protected void onScheduleEviction(EvictibleEntry<K, V> e) {
        if (getNextEvictionTime() != next) {
            scheduleTask();
        }
    }


    @Override
    protected void onCancelEviction(EvictibleEntry<K, V> e) {
        if (getNextEvictionTime() != next) {
            scheduleTask();
        }
    }


    @Override
    protected void onEvictEntries() {
        schedule();
    }

    /**
     * (Re)schedules the task atomically. This method is synchronized to ensure
     * atomicity.
     */
    private synchronized void scheduleTask() {
        // Cancel the task if already scheduled, then call schedule() to
        // schedule a new task.
        if (future != null) {
            future.cancel(false);
        }

        schedule();
    }

    /**
     * Schedules the task atomically. This method is synchronized to ensure
     * atomicity. The next eviction time is always queried within this
     * synchronized method and not passed as parameter to ensure consistency in
     * a concurrent environment.
     */
    private synchronized void schedule() {
        // Get the next eviction time and reschedule the task with a delay
        // corresponding to the
        // difference between this time and the current time. If the next
        // eviction time is 0
        // (the queue is empty), don't schedule anything.
        next = getNextEvictionTime();
        future = (next > 0) ? ses.schedule(new EvictionRunnable(), Math.max(next - System.nanoTime(), 0), NANOSECONDS) : null;
    }
}
