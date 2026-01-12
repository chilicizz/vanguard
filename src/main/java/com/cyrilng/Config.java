package com.cyrilng;

import com.cyrilng.vanguard.rss.mongo.MongoStorage;
import com.cyrilng.vanguard.rss.mongo.MongoUtils;
import com.mongodb.reactivestreams.client.MongoClient;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class Config {

    public static final String CYRSS_DB = "cyrss";

    @Bean
    @Singleton
    public MongoClient createMongoClient() {
        return MongoUtils.createClient(System.getenv(MongoUtils.MONGO_CONNECTION_STRING));
    }

    @Bean
    @Singleton
    public MongoStorage createMongoInterface(MongoClient mongoClient) {
        return new MongoStorage(mongoClient, CYRSS_DB);
    }
}
