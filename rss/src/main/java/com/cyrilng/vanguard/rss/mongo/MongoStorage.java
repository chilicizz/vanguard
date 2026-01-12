package com.cyrilng.vanguard.rss.mongo;

import com.cyrilng.vanguard.rss.domain.Entry;
import com.cyrilng.vanguard.rss.domain.RssFeed;
import com.cyrilng.vanguard.rss.domain.RssUser;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.FindPublisher;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.BsonType;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

public class MongoStorage implements StorageInterface {
    private static final Logger logger = LoggerFactory.getLogger(MongoStorage.class);
    private final MongoClient mongoClient;
    private final String databaseName;
    private final MongoDatabase database;

    public MongoStorage(MongoClient mongoClient, String databaseName) {
        this.mongoClient = mongoClient;
        this.databaseName = databaseName;
        this.database = mongoClient.getDatabase(databaseName);
        logger.info("Initialised");
    }

    @Override
    public CompletableFuture<RssUser> fetchUserById(String userId) {
        MongoCollection<RssUser> collection = database.getCollection(Constants.USER_COLLECTION, RssUser.class);
        FindPublisher<RssUser> response = collection.find(eq(new ObjectId(userId)));
        return AsyncUtils.singleResultFrom(response);
    }

    @Override
    public CompletableFuture<RssUser> fetchUserByUsername(String username) {
        MongoCollection<RssUser> collection = database.getCollection(Constants.USER_COLLECTION, RssUser.class);
        FindPublisher<RssUser> response = collection.find(eq("username", username));
        return AsyncUtils.singleResultFrom(response);
    }

    @Override
    public CompletableFuture<String> createNewUser(String username) {
        MongoCollection<RssUser> collection = database.getCollection(Constants.USER_COLLECTION, RssUser.class);
        Publisher<InsertOneResult> response = collection.insertOne(new RssUser(null, username, null));
        return AsyncUtils.singleResultFrom(response).thenApply(insertOneResult -> {
            if (insertOneResult.getInsertedId() != null && BsonType.OBJECT_ID.equals(insertOneResult.getInsertedId().getBsonType())) {
                return String.valueOf(insertOneResult.getInsertedId().asObjectId().getValue());
            }
            throw new RuntimeException("Unexpected ID: " + insertOneResult.getInsertedId());
        });
    }

    @Override
    public CompletableFuture<Boolean> updateUserRssFeeds(String userId, List<String> feedUrls) {
        MongoCollection<RssUser> collection = database.getCollection(Constants.USER_COLLECTION, RssUser.class);
        Publisher<UpdateResult> response = collection.updateOne(eq(new ObjectId(userId)), Updates.set("rssFeedUrls", feedUrls));
        return AsyncUtils.singleResultFrom(response).thenApply(UpdateResult::wasAcknowledged);
    }

    @Override
    public CompletableFuture<Boolean> deleteUser(String userId) {
        MongoCollection<RssUser> collection = database.getCollection(Constants.USER_COLLECTION, RssUser.class);
        return AsyncUtils.singleResultFrom(collection.deleteOne(eq(new ObjectId(userId)))).thenApply(DeleteResult::wasAcknowledged);
    }

    @Override
    public CompletableFuture<List<RssFeed>> fetchFeeds(String... feedUrls) {
        MongoCollection<RssFeed> collection = database.getCollection(Constants.FEEDS_COLLECTION, RssFeed.class);
        FindPublisher<RssFeed> response = collection.find(Filters.or(Arrays.stream(feedUrls).map(feedUrl -> eq("feedUrl", feedUrl)).toArray(Bson[]::new)));
        return AsyncUtils.multipleResultsFrom(response);
    }

    @Override
    public CompletableFuture<Map<Integer, String>> createNewFeeds(RssFeed... rssFeeds) {
        MongoCollection<RssFeed> collection = database.getCollection(Constants.FEEDS_COLLECTION, RssFeed.class);
        Publisher<InsertManyResult> response = collection.insertMany(List.of(rssFeeds));
        return AsyncUtils.singleResultFrom(response).thenApply(insertManyResult -> {
            if (insertManyResult.wasAcknowledged()) {
                return insertManyResult.getInsertedIds().entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey, integerBsonValueEntry -> String.valueOf(integerBsonValueEntry.getValue().asObjectId().getValue())
                ));
            } else {
                return Collections.emptyMap();
            }
        });
    }

    @Override
    public CompletableFuture<Map<Integer, String>> createNewEntries(Entry... feedEntries) {
        MongoCollection<Entry> collection = database.getCollection(Constants.FEEDS_COLLECTION, Entry.class);
        Publisher<InsertManyResult> response = collection.insertMany(List.of(feedEntries));
        return AsyncUtils.singleResultFrom(response).thenApply(insertManyResult -> {
            if (insertManyResult.wasAcknowledged()) {
                return insertManyResult.getInsertedIds().entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey, integerBsonValueEntry -> String.valueOf(integerBsonValueEntry.getValue().asObjectId().getValue())
                ));
            } else {
                return Collections.emptyMap();
            }
        });
    }

    @Override
    public void createIndexes() {
        MongoCollection<RssUser> users = database.getCollection(Constants.USER_COLLECTION, RssUser.class);
        users.createIndex(Indexes.ascending("username"), new IndexOptions().unique(true));

        MongoCollection<RssFeed> feeds = database.getCollection(Constants.FEEDS_COLLECTION, RssFeed.class);
        feeds.createIndex(Indexes.ascending("feedURL"), new IndexOptions().unique(true));

        MongoCollection<Entry> entries = database.getCollection(Constants.ENTRIES_COLLECTION, Entry.class);
        entries.createIndex(Indexes.descending("feedId", "pubDate"));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "databaseName='" + databaseName + '\'' +
                '}';
    }
}
