package com.cyrilng;

import io.grpc.stub.StreamObserver;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;

@MicronautTest
public class MessageStreamTest {
    private static final Logger logger = LoggerFactory.getLogger(MessageStreamTest.class);

    @Inject
    VanguardServiceGrpc.VanguardServiceStub vanguardServiceStub;

    @Test
    void testGrpcStream() throws InterruptedException {
        AtomicBoolean received = new AtomicBoolean(false);

        VanguardMessage message = VanguardMessage.newBuilder().setMessage("Test Message").build();
        StreamObserver<VanguardMessage> messageStreamObserver = Mockito.spy(new StreamObserver<>() {
            @Override
            public void onNext(VanguardMessage value) {
                logger.info("Test observer received message {}", value);
                received.set(true);
            }

            @Override
            public void onError(Throwable t) {
                logger.info("Test observer received throwable {}", t, t);
            }

            @Override
            public void onCompleted() {
                logger.info("Test observer onCompleted ");
            }
        });

        StreamObserver<VanguardMessage> messageSender = vanguardServiceStub.joinChat(messageStreamObserver);
        logger.info("Sending test message: {}", message);
        messageSender.onNext(message);
        Awaitility.await().until(received::get);
        messageSender.onCompleted();
        Mockito.verify(messageStreamObserver, Mockito.atLeastOnce()).onNext(any(VanguardMessage.class));
    }
}
