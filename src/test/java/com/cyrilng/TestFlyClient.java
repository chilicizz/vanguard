package com.cyrilng;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFlyClient {
    private static final Logger logger = LoggerFactory.getLogger(TestFlyClient.class);

    public static void main(String[] args) {
        logger.info("Setting up channel");
        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("vanguard.fly.dev", 443).useTransportSecurity().build();
//        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", 8080).usePlaintext().build();

        logger.info("Creating grpc stub with " + managedChannel);
        VanguardServiceGrpc.VanguardServiceBlockingStub blockingStub = VanguardServiceGrpc.newBlockingStub(managedChannel);

        VanguardRequest request = VanguardRequest.newBuilder().setName("Tester").build();
        logger.info("Sending request " + request);

        VanguardReply response = blockingStub.send(request);
        logger.info("Received response " + response);
        assertEquals(
                "Hello Tester",
                response.getMessage());

        
    }
}
