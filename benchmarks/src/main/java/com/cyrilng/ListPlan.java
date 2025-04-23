package com.cyrilng;

import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

@State(Scope.Benchmark)
public class ListPlan {
    @Param
    public ListSample listSample;

    public enum ListSample {
        ARRAY_LIST(ArrayList::new),
        COPY_ON_WRITE_ARRAY_LIST(CopyOnWriteArrayList::new),
        LINKED_LIST(LinkedList::new);

        private final Supplier<List<Integer>> constructor;

        ListSample(Supplier<List<Integer>> constructor) {
            this.constructor = constructor;
        }

        List<Integer> get() {
            return constructor.get();
        }
    }
}
