package com.cyrilng;

import com.cyrilng.VanguardServiceGrpc.VanguardServiceImplBase;
import io.grpc.stub.StreamObserver;
import io.micronaut.grpc.annotation.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

@GrpcService
public class VanguardServiceImpl extends VanguardServiceImplBase implements AutoCloseable {
    public static final Logger logger = LoggerFactory.getLogger(VanguardServiceImpl.class);
    private final Map<Long, StreamObserver<VanguardMessage>> subscribers;
    private final BlockingQueue<VanguardMessage> messageQueue;
    private final AtomicLong idProvider;
    private final Thread messageHandlerThread;

    public VanguardServiceImpl() {
        idProvider = new AtomicLong();
        subscribers = new ConcurrentHashMap<>();
        messageQueue = new LinkedBlockingQueue<>();
        messageHandlerThread = new Thread(() -> {
            try {
                while (true) {
                    VanguardMessage message = messageQueue.take();
                    logger.debug("Received message from queue: " + message);
                    subscribers.forEach((id, vanguardMessageStreamObserver) -> {
                        logger.trace("Publishing message to subscriber " + id);
//                        CompletableFuture.runAsync(() -> vanguardMessageStreamObserver.onNext(message));
                        vanguardMessageStreamObserver.onNext(message);
                    });
                }
            } catch (InterruptedException e) {
                logger.error("Terminating thread: " + e.getMessage(), e);
            }
        });
        logger.info("Starting message handler thread");
        messageHandlerThread.setDaemon(true);
        messageHandlerThread.start();
    }

    @Override
    public void send(VanguardRequest request, StreamObserver<VanguardReply> responseObserver) {
        logger.info("Received message: " + request);
        String name = request.getName();
        VanguardReply vanguardReply = VanguardReply.newBuilder().setMessage("Hello " + name).build();
        responseObserver.onNext(vanguardReply);
        responseObserver.onCompleted();
        logger.info("Completed");
    }

    @Override
    public StreamObserver<VanguardMessage> joinChat(StreamObserver<VanguardMessage> responseObserver) {
        final long subscriptionId = idProvider.getAndIncrement();
        logger.info("Registering new subscriber: " + subscriptionId + " " + responseObserver);
        subscribers.put(subscriptionId, responseObserver);
        return new StreamObserver<>() {
            final long id = subscriptionId;

            @Override
            public void onNext(VanguardMessage message) {
                logger.debug("Adding message to queue: " + message);
                messageQueue.add(message);
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Error " + t.getMessage(), t);
                subscribers.remove(id);
            }

            @Override
            public void onCompleted() {
                logger.info("Removing subscriber: " + id);
                subscribers.remove(id);
            }
        };
    }

    @Override
    public void close() throws Exception {
        logger.info("Closing");
        messageHandlerThread.interrupt();
        messageQueue.clear();
        subscribers.forEach((integer, vanguardMessageStreamObserver) -> {
            logger.debug("Closing subscription: " + integer);
            vanguardMessageStreamObserver.onCompleted();
        });
    }
}
