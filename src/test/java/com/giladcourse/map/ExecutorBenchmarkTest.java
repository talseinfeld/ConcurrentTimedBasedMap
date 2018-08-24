package com.giladcourse.map;

import com.giladcourse.scheduler.ExecutorServiceEvictionScheduler;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class ExecutorBenchmarkTest {

    private ConcurrentHashMapWithTimedEviction<Integer, String> map;

    private static final Integer ITERATIONS = 100000;

    private ExecutorServiceEvictionScheduler ses;

    @Setup(Level.Iteration)
    public void setup() {
        ses = new ExecutorServiceEvictionScheduler<Integer, String>();
        map = new ConcurrentHashMapWithTimedEviction<Integer, String>(ses);
    }

    @TearDown(Level.Iteration)
    public void tearDown(){
        ses.shutdown();
    }

    @Benchmark
    public void testPut() throws InterruptedException {
        populateMap();
    }

    @Benchmark
    public void testGet() throws InterruptedException {
        populateMap();
        for (int i = 0; i < ITERATIONS; i++) {
            map.get(i);
        }
    }

    private void populateMap(){
        for (int i = 0; i < ITERATIONS; i++) {
            map.put(i, "value " + i, 5 + i);
        }
    }
}
