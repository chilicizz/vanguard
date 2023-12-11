package com.cyrilng;

import com.cyrilng.VanguardServiceGrpc.VanguardServiceBlockingStub;
import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        VanguardReply reply = blockingStub.send(request);
        assertEquals(
                "Hello Tester",
                reply.getMessage());
    }


}
