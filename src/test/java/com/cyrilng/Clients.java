package com.cyrilng;

import io.grpc.ManagedChannel;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.grpc.annotation.GrpcChannel;
import io.micronaut.grpc.server.GrpcServerChannel;

@Factory
class Clients {
    @Bean
    VanguardServiceGrpc.VanguardServiceBlockingStub blockingStub(
            @GrpcChannel(GrpcServerChannel.NAME) ManagedChannel channel) { 
        return VanguardServiceGrpc.newBlockingStub( 
                channel
        );
    }
}
