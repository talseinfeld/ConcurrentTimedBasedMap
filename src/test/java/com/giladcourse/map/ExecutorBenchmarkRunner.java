package com.giladcourse.map;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class ExecutorBenchmarkRunner {

    public static void main(String... args) throws RunnerException {
        Options opts = new OptionsBuilder()
                .measurementIterations(50)
                .warmupIterations(50)
                .forks(1)
                .threads(4)
                .jvmArgs("-Xms1g", "-Xmx1g", "-Xmn800m", "-server")
                .include(ExecutorBenchmarkTest.class.getSimpleName())
                .build();

        new Runner(opts).run();
    }
}
