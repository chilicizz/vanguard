package com.cyrilng;

import org.openjdk.jmh.annotations.*;

import java.util.List;

@Fork(warmups = 0, value = 1)
@Warmup(iterations = 0)
@Measurement(iterations = 3)
public class DisruptorTest {

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OperationsPerInvocation(1_000)
    public List<Integer> runTest(ListPlan plan) {
        List<Integer> list = plan.listSample.get();
        for (int i = 0; i < 1_000; i++) {
            list.add(i);
        }
        return list;
    }
}
