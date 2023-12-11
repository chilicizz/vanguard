package com.cyrilng;

import io.grpc.stub.StreamObserver;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.ArgumentMatchers.any;

@MicronautTest
public class MessageStreamTest {
    private static final Logger logger = LoggerFactory.getLogger(MessageStreamTest.class);

    @Inject
    VanguardServiceGrpc.VanguardServiceStub vanguardServiceStub;

    @Test
    void testGrpcStream() throws InterruptedException {
        VanguardMessage message = VanguardMessage.newBuilder().setMessage("Test Message").build();
        StreamObserver<VanguardMessage> messageStreamObserver = Mockito.spy(vanguardServiceStub.joinChat(new StreamObserver<>() {
            @Override
            public void onNext(VanguardMessage value) {
                logger.info("Test observer received message " + value);
            }

            @Override
            public void onError(Throwable t) {
                logger.info("Test observer received throwable " + t, t);
            }

            @Override
            public void onCompleted() {
                logger.info("Test observer onCompleted ");
            }
        }));

        StreamObserver<VanguardMessage> messageSender = vanguardServiceStub.joinChat(messageStreamObserver);
        logger.info("Sending test message: " + message);
        messageSender.onNext(message);

        Thread.sleep(1000);
        Mockito.verify(messageStreamObserver, Mockito.atLeastOnce()).onNext(
                any(VanguardMessage.class)
        );
        messageSender.onCompleted();
    }
}
