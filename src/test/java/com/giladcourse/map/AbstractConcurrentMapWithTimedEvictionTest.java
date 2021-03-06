package com.giladcourse.map;

import com.giladcourse.EvictionScheduler;
import com.giladcourse.scheduler.DelayedTaskEvictionScheduler;
import com.giladcourse.scheduler.ExecutorServiceEvictionScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.assertTrue;


public abstract class AbstractConcurrentMapWithTimedEvictionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConcurrentMapWithTimedEvictionTest.class);

    public static final String VALUE = "value";

    public static final String VALUE2 = "valuex";

    public static final long TIMEOUT_MS = 60 * 60 * 1000;

    public static final int MAX_EVICTION_THREADS = 1;

    public static final int MAX_MAP_SIZE = 1000000;

    public static final float LOAD_FACTOR = 0.75f;

    public static final int RESULT_PASSED = 0;

    public static final int RESULT_INTERRUPTED = -1;

    public static final int RESULT_ASSERTION_FAILED = -2;

    public static final int IMPL_CHM = 0;

    public static final int IMPL_CHMWTE_ESS = 1; // ExecutionService

    public static final int IMPL_CHMWTE_NM_DT = 2; // DelayedTask with NavigableMap

    protected final int impl;

    protected final long evictMs;

    protected final int numThreads;

    protected final int numIterations;

    protected ScheduledThreadPoolExecutor evictionExecutor;

    protected EvictionScheduler<Integer, String> scheduler;

    protected ConcurrentMap<Integer, String> map;

    protected ThreadPoolExecutor testExecutor;

    public AbstractConcurrentMapWithTimedEvictionTest(int impl, long evictMs, int numThreads, int numIterations) {
        this.impl = impl;
        this.evictMs = evictMs;
        this.numThreads = numThreads;
        this.numIterations = numIterations;
    }

    public void setUp() throws Exception {
        createExecutor();
        createScheduler();
        createMap();
        createTestExecutor();
    }

    public void tearDown() throws Exception {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    public abstract void setUpIteration();

    public abstract void tearDownIteration();

    protected void createExecutor() {
        evictionExecutor = new ScheduledThreadPoolExecutor(MAX_EVICTION_THREADS);

    }

    protected void createScheduler() {
        switch (impl) {
            case IMPL_CHMWTE_ESS:
                scheduler = new ExecutorServiceEvictionScheduler<Integer, String>(evictionExecutor);
                break;
            case IMPL_CHMWTE_NM_DT:
                scheduler = new DelayedTaskEvictionScheduler<Integer, String>(evictionExecutor);
                break;

        }
    }

    protected void createMap() {
        int capacity = Math.min(numThreads * numIterations, MAX_MAP_SIZE);
        switch (impl) {
            case IMPL_CHM:
                map = new ConcurrentHashMap<Integer, String>(capacity, LOAD_FACTOR, numThreads);
                break;
            case IMPL_CHMWTE_ESS:
            case IMPL_CHMWTE_NM_DT:
                map = new ConcurrentHashMapWithTimedEviction<Integer, String>(capacity, LOAD_FACTOR, numThreads, scheduler);
                break;
        }
    }

    private void createTestExecutor() {
        testExecutor = new ThreadPoolExecutor(numThreads, Integer.MAX_VALUE, 0, NANOSECONDS, new ArrayBlockingQueue<Runnable>(numThreads, true));
    }

    protected interface TestTask {
        void test(int id) throws InterruptedException;
    }

    protected void run(String name, TestTask task) throws InterruptedException {
        printHeader(name);
        TestRunnable[] runnables = createRunnables(task);
        executeRunnables(runnables);
        checkResults(runnables);
        printAverageTime(calcAverageTime(runnables));
    }

    private TestRunnable[] createRunnables(TestTask task) {
        TestRunnable[] runnables = new TestRunnable[numThreads];
        for (int i = 0; i < numThreads; i++) {
            runnables[i] = new TestRunnable(i, task);
        }
        return runnables;
    }

    private void executeRunnables(TestRunnable[] runnables) throws InterruptedException {
        for (int i = 0; i < numThreads; i++) {
            testExecutor.submit(runnables[i]);
        }
        testExecutor.shutdown();
        assertTrue(testExecutor.awaitTermination(TIMEOUT_MS, MILLISECONDS));
    }

    private void checkResults(TestRunnable[] runnables) throws AssertionError {
        for (TestRunnable r : runnables) {
            if (r.getError() != null) {
                throw r.getError();
            }
            assertTrue(r.getResult() == RESULT_PASSED);
        }
    }

    private double calcAverageTime(TestRunnable[] runnables) {
        long sum = 0;
        for (TestRunnable r : runnables) {
            sum += r.getDurationNs();
        }
        return ((double) sum) / (numThreads * numIterations * 1000.0);
    }

    private void printHeader(String name) {
        // @formatter:off
        System.out.printf("%s(%s), %d ms, %d threads, %d iterations: %s\n", map.getClass().getSimpleName(), ((scheduler != null) ? scheduler.getClass().getSimpleName() : ""),
                evictMs, numThreads, numIterations, name);
        // @formatter:on
    }

    private void printAverageTime(double avgTime) {
        System.out.printf("Average time: %f us\n", avgTime);
    }

    private final class TestRunnable implements Runnable {

        private final int id;
        private final TestTask task;
        private long durationNs = 0;
        private int result = RESULT_PASSED;
        private AssertionError error = null;

        public TestRunnable(int id, TestTask task) {
            super();
            this.id = id;
            this.task = task;
        }

        public long getDurationNs() {
            return durationNs;
        }

        public int getResult() {
            return result;
        }

        public AssertionError getError() {
            return error;
        }

        @Override
        public void run() {
            for (int i = 0; i < numIterations; i++) {
                setUpIteration();
                int idx = getIterationId(id, i);
                try {
                    long startNs = System.nanoTime();
                    task.test(idx);
                    long endNs = System.nanoTime();
                    durationNs += (endNs - startNs);
                } catch (InterruptedException e) {
                    result = RESULT_INTERRUPTED;
                    break;
                } catch (AssertionError e) {
                    result = RESULT_ASSERTION_FAILED;
                    error = e;
                    System.out.printf("Assertion failed: %d\n", idx);
                    break;
                }
                tearDownIteration();
            }
        }
    }

    protected int getIterationId(int id, int i) {
        return (id * numIterations + i) % MAX_MAP_SIZE;
    }

    protected static String getValue(int id) {
        return VALUE + id;
    }

    protected static String getValue2(int id) {
        return VALUE2 + id;
    }

    protected static int getId2(int id) {
        return Integer.MAX_VALUE - id;
    }

}
