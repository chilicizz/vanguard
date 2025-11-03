package com.cyrilng;

import com.cyrilng.vanguard.rss.mongo.MongoInterface;
import com.cyrilng.vanguard.rss.mongo.MongoUtils;
import com.mongodb.reactivestreams.client.MongoClient;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class Config {

    @Bean
    @Singleton
    public MongoClient createMongoClient() {
        return MongoUtils.createClient(System.getenv("MONGO_CONNECTION_STRING"));
    }

    @Bean
    @Singleton
    public MongoInterface createMongoInterface(MongoClient mongoClient) {
        return new MongoInterface(mongoClient, "cyrss");
    }
}
