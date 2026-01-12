package com.cyrilng.vanguard.rss.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AsyncUtils {

    public static MongoClient createClient(String connectionString) {
        assert connectionString != null && !connectionString.isEmpty() : "MONGO_CONNECTION_STRING environment variable is not set";
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .serverApi(serverApi)
                .build();
        return MongoClients.create(settings);
    }

    public static <T> CompletableFuture<T> singleResultFrom(Publisher<T> publisher) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        publisher.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(T t) {
                completableFuture.complete(t);
            }

            @Override
            public void onError(Throwable t) {
                completableFuture.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                if (!completableFuture.isDone()) {
                    completableFuture.complete(null);
                }
            }
        });
        return completableFuture;
    }

    public static <T> CompletableFuture<List<T>> multipleResultsFrom(Publisher<T> publisher) {
        CompletableFuture<List<T>> completableFuture = new CompletableFuture<>();
        publisher.subscribe(new Subscriber<>() {
            private Subscription subscription;
            private List<T> results;

            @Override
            public void onSubscribe(Subscription s) {
                results = new ArrayList<>();
                subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(T t) {
                results.add(t);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable t) {
                completableFuture.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                completableFuture.complete(results);
            }
        });
        return completableFuture;
    }
}
