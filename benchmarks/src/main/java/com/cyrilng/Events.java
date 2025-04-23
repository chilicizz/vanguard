package com.cyrilng;

import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class Events {
    @Param({"100", "200"})
    public int iterations;

}
