package com.cyrilng.vanguard.rss.mongo;

import com.cyrilng.vanguard.rss.domain.FeedEntry;
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

public class MongoInterface {
    public static final String USER_COLLECTION = "user";
    public static final String FEEDS_COLLECTION = "feeds";
    public static final String ENTRIES_COLLECTION = "entries";
    private static final Logger logger = LoggerFactory.getLogger(MongoInterface.class);
    private final MongoClient mongoClient;
    private final String databaseName;

    MongoInterface(MongoClient mongoClient, String databaseName) {
        this.mongoClient = mongoClient;
        this.databaseName = databaseName;
        logger.info("Initialised");
    }

    public MongoInterface() {
        this(MongoUtils.createClient(System.getenv("MONGO_CONNECTION_STRING")), "cyrss");
    }

    public CompletableFuture<RssUser> fetchUserById(String userId) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<RssUser> collection = database.getCollection(USER_COLLECTION, RssUser.class);
        FindPublisher<RssUser> response = collection.find(eq(new ObjectId(userId)));
        return MongoUtils.singleResultFrom(response);
    }

    public CompletableFuture<RssUser> fetchUserByUsername(String username) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<RssUser> collection = database.getCollection(USER_COLLECTION, RssUser.class);
        FindPublisher<RssUser> response = collection.find(eq("username", username));
        return MongoUtils.singleResultFrom(response);
    }

    public CompletableFuture<String> createNewUser(String username) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<RssUser> collection = database.getCollection(USER_COLLECTION, RssUser.class);
        Publisher<InsertOneResult> response = collection.insertOne(new RssUser(null, username, null));
        return MongoUtils.singleResultFrom(response).thenApply(insertOneResult -> {
            if (insertOneResult.getInsertedId() != null && BsonType.OBJECT_ID.equals(insertOneResult.getInsertedId().getBsonType())) {
                return String.valueOf(insertOneResult.getInsertedId().asObjectId().getValue());
            }
            throw new RuntimeException("Unexpected ID: " + insertOneResult.getInsertedId());
        });
    }

    public CompletableFuture<Boolean> updateUserRssFeeds(String userId, List<String> feedUrls) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<RssUser> collection = database.getCollection(USER_COLLECTION, RssUser.class);
        Publisher<UpdateResult> response = collection.updateOne(eq(new ObjectId(userId)), Updates.set("rssFeedUrls", feedUrls));
        return MongoUtils.singleResultFrom(response).thenApply(UpdateResult::wasAcknowledged);
    }

    public CompletableFuture<Boolean> deleteUser(String userId) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<RssUser> collection = database.getCollection(USER_COLLECTION, RssUser.class);
        return MongoUtils.singleResultFrom(collection.deleteOne(eq(new ObjectId(userId)))).thenApply(DeleteResult::wasAcknowledged);
    }

    public CompletableFuture<List<RssFeed>> fetchFeeds(String... feedUrls) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<RssFeed> collection = database.getCollection(FEEDS_COLLECTION, RssFeed.class);
        FindPublisher<RssFeed> response = collection.find(Filters.or(Arrays.stream(feedUrls).map(feedUrl -> eq("feedUrl", feedUrl)).toArray(Bson[]::new)));
        return MongoUtils.multipleResultsFrom(response);
    }

    public CompletableFuture<Map<Integer, String>> createNewFeeds(RssFeed... rssFeeds) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<RssFeed> collection = database.getCollection(FEEDS_COLLECTION, RssFeed.class);
        Publisher<InsertManyResult> response = collection.insertMany(List.of(rssFeeds));
        return MongoUtils.singleResultFrom(response).thenApply(insertManyResult -> {
            if (insertManyResult.wasAcknowledged()) {
                return insertManyResult.getInsertedIds().entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey, integerBsonValueEntry -> String.valueOf(integerBsonValueEntry.getValue().asObjectId().getValue())
                ));
            } else {
                return Collections.emptyMap();
            }
        });
    }

    public CompletableFuture<Map<Integer, String>> createNewEntries(FeedEntry... feedEntries) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<FeedEntry> collection = database.getCollection(FEEDS_COLLECTION, FeedEntry.class);
        Publisher<InsertManyResult> response = collection.insertMany(List.of(feedEntries));
        return MongoUtils.singleResultFrom(response).thenApply(insertManyResult -> {
            if (insertManyResult.wasAcknowledged()) {
                return insertManyResult.getInsertedIds().entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey, integerBsonValueEntry -> String.valueOf(integerBsonValueEntry.getValue().asObjectId().getValue())
                ));
            } else {
                return Collections.emptyMap();
            }
        });
    }

    public void createIndexes() {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<RssUser> users = database.getCollection(USER_COLLECTION, RssUser.class);
        users.createIndex(Indexes.ascending("username"), new IndexOptions().unique(true));

        MongoCollection<RssFeed> feeds = database.getCollection(FEEDS_COLLECTION, RssFeed.class);
        feeds.createIndex(Indexes.ascending("feedURL"), new IndexOptions().unique(true));

        MongoCollection<FeedEntry> entries = database.getCollection(ENTRIES_COLLECTION, FeedEntry.class);
        entries.createIndex(Indexes.descending("feedId", "pubDate"));
    }
}
