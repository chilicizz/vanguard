package com.cyrilng;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

public class TestFlyClient {
    private static final Logger logger = LoggerFactory.getLogger(TestFlyClient.class);
    private ManagedChannel managedChannel;

    @BeforeEach
    public void setUp() {
        logger.info("Setting up channel");
        managedChannel = ManagedChannelBuilder.forAddress("vanguard.fly.dev", 443).useTransportSecurity().build();
//        managedChannel = ManagedChannelBuilder.forAddress("localhost", 8080).usePlaintext().build();
    }

    @Test
    public void runBlockingMessageRequest() {
        logger.info("Creating grpc stub with " + managedChannel);
        VanguardServiceGrpc.VanguardServiceBlockingStub blockingStub = VanguardServiceGrpc.newBlockingStub(managedChannel);

        VanguardRequest request = VanguardRequest.newBuilder().setName("Tester").build();
        logger.info("Sending request " + request);

        VanguardReply response = blockingStub.send(request);
        logger.info("Received response " + response);
        assertEquals("Hello Tester", response.getMessage());
    }

    @Test
    public void testStreamingMessageRequest() throws InterruptedException {
        AtomicBoolean received = new AtomicBoolean(false);
        VanguardServiceGrpc.VanguardServiceStub vanguardServiceStub = VanguardServiceGrpc.newStub(managedChannel);

        VanguardMessage message = VanguardMessage.newBuilder().setMessage("Test Message").build();

        StreamObserver<VanguardMessage> messageStreamObserver = Mockito.spy(new StreamObserver<>() {
            @Override
            public void onNext(VanguardMessage value) {
                logger.info("Test observer received message " + value);
                received.set(true);
            }

            @Override
            public void onError(Throwable t) {
                logger.info("Test observer received throwable " + t, t);
            }

            @Override
            public void onCompleted() {
                logger.info("Test observer onCompleted ");
            }
        });

        StreamObserver<VanguardMessage> messageSender = vanguardServiceStub.joinChat(messageStreamObserver);
        logger.info("Sending test message: " + message);
        messageSender.onNext(message);

        Awaitility.await().until(received::get);
        messageSender.onCompleted();

        Mockito.verify(messageStreamObserver, Mockito.atLeastOnce()).onNext(
                any(VanguardMessage.class)
        );
    }
}
