package com.cyrilng;

import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import com.cyrilng.VanguardServiceGrpc.VanguardServiceBlockingStub;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;

import jakarta.inject.Inject;

@MicronautTest
class VanguardTest {

    @Inject
    EmbeddedApplication<?> application;

    @Inject
    VanguardServiceBlockingStub blockingStub;

    @Test
    void testItWorks() {
        Assertions.assertTrue(application.isRunning());
    }

    @Test
    void testGrpc() {
        VanguardRequest request = VanguardRequest.newBuilder().setName("Tester").build();

        assertEquals(
                "Hello Tester",
                blockingStub.send(request).getMessage());
    }
}
