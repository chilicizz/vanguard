package com.cyrilng;

import com.cyrilng.vanguard.rss.mongo.Constants;
import com.cyrilng.vanguard.rss.mongo.MongoStorage;
import com.cyrilng.vanguard.rss.mongo.AsyncUtils;
import com.mongodb.reactivestreams.client.MongoClient;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class Config {

    @Bean
    @Singleton
    public MongoClient createMongoClient() {
        return AsyncUtils.createClient(System.getenv(Constants.MONGO_CONNECTION_STRING));
    }

    @Bean
    @Singleton
    public MongoStorage createMongoInterface(MongoClient mongoClient) {
        return new MongoStorage(mongoClient, Constants.CYRSS_DB);
    }
}
