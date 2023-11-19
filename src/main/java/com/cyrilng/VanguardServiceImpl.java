package com.cyrilng;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cyrilng.VanguardServiceGrpc.VanguardServiceImplBase;

import io.grpc.stub.StreamObserver;
import io.micronaut.grpc.annotation.GrpcService;

@GrpcService
public class VanguardServiceImpl extends VanguardServiceImplBase {
    public static final Logger logger = LoggerFactory.getLogger(VanguardServiceImpl.class);

    public VanguardServiceImpl(){
        logger.info("init");
    }

    @Override
    public void send(VanguardRequest request, StreamObserver<VanguardReply> responseObserver) {
        logger.info("Received message " + request);
        String name = request.getName();
        VanguardReply vanguardReply = VanguardReply.newBuilder().setMessage("Hello " + name).build();
        responseObserver.onNext(vanguardReply);
        responseObserver.onCompleted();
    }
    
}
