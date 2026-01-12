package com.cyrilng.vanguard.rss.mongo;

import com.cyrilng.vanguard.rss.domain.Entry;
import com.cyrilng.vanguard.rss.domain.RssFeed;
import com.cyrilng.vanguard.rss.domain.RssUser;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonType;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;

/**
 * Synchronous (blocking) implementation of {@link StorageInterface} that delegates blocking
 * MongoDB calls to a background executor and returns CompletableFutures.
 */
public class MongoStorageSync implements StorageInterface {
    private static final Logger logger = LoggerFactory.getLogger(MongoStorageSync.class);

    public static final String USER_COLLECTION = "user";
    public static final String FEEDS_COLLECTION = "feeds";
    public static final String ENTRIES_COLLECTION = "entries";

    private final MongoClient mongoClient;
    private final String databaseName;
    private final MongoDatabase database;
    private final ExecutorService executor;

    public MongoStorageSync(MongoClient mongoClient, String databaseName) {
        this(mongoClient, databaseName, Executors.newCachedThreadPool());
    }

    public MongoStorageSync(MongoClient mongoClient, String databaseName, ExecutorService executor) {
        this.mongoClient = mongoClient;
        this.databaseName = databaseName;
        this.database = mongoClient.getDatabase(databaseName);
        this.executor = executor;
        logger.info("Initialised synchronous MongoStorage for database={}", databaseName);
    }

    @Override
    public CompletableFuture<RssUser> fetchUserById(String userId) {
        if (userId == null) return CompletableFuture.completedFuture(null);
        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<RssUser> collection = database.getCollection(USER_COLLECTION, RssUser.class);
            try {
                return collection.find(eq("_id", new ObjectId(userId))).first();
            } catch (Exception e) {
                logger.warn("fetchUserById failed for userId={}", userId, e);
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<RssUser> fetchUserByUsername(String username) {
        if (username == null) return CompletableFuture.completedFuture(null);
        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<RssUser> collection = database.getCollection(USER_COLLECTION, RssUser.class);
            try {
                return collection.find(eq("username", username)).first();
            } catch (Exception e) {
                logger.warn("fetchUserByUsername failed for username={}", username, e);
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<String> createNewUser(String username) {
        if (username == null) return CompletableFuture.failedFuture(new IllegalArgumentException("username is null"));
        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<RssUser> collection = database.getCollection(USER_COLLECTION, RssUser.class);
            try {
                InsertOneResult result = collection.insertOne(new RssUser(null, username, null));
                if (result.getInsertedId() != null && BsonType.OBJECT_ID.equals(result.getInsertedId().getBsonType())) {
                    return result.getInsertedId().asObjectId().getValue().toHexString();
                }
                throw new RuntimeException("Unexpected InsertedId: " + result.getInsertedId());
            } catch (Exception e) {
                logger.warn("createNewUser failed for username={}", username, e);
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> updateUserRssFeeds(String userId, List<String> feedUrls) {
        if (userId == null) return CompletableFuture.failedFuture(new IllegalArgumentException("userId is null"));
        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<RssUser> collection = database.getCollection(USER_COLLECTION, RssUser.class);
            try {
                UpdateResult result = collection.updateOne(eq("_id", new ObjectId(userId)), Updates.set("rssFeedUrls", feedUrls));
                return result.wasAcknowledged();
            } catch (Exception e) {
                logger.warn("updateUserRssFeeds failed for userId={}", userId, e);
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> deleteUser(String userId) {
        if (userId == null) return CompletableFuture.completedFuture(false);
        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<RssUser> collection = database.getCollection(USER_COLLECTION, RssUser.class);
            try {
                DeleteResult result = collection.deleteOne(eq("_id", new ObjectId(userId)));
                return result.wasAcknowledged();
            } catch (Exception e) {
                logger.warn("deleteUser failed for userId={}", userId, e);
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<RssFeed>> fetchFeeds(String... feedUrls) {
        if (feedUrls == null || feedUrls.length == 0) return CompletableFuture.completedFuture(Collections.emptyList());
        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<RssFeed> collection = database.getCollection(FEEDS_COLLECTION, RssFeed.class);
            try {
                Bson[] ors = Arrays.stream(feedUrls).map(feedUrl -> eq("feedUrl", feedUrl)).toArray(Bson[]::new);
                return collection.find(or(ors)).into(new java.util.ArrayList<>());
            } catch (Exception e) {
                logger.warn("fetchFeeds failed for feedUrls={}", Arrays.toString(feedUrls), e);
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Map<Integer, String>> createNewFeeds(RssFeed... rssFeeds) {
        if (rssFeeds == null || rssFeeds.length == 0) return CompletableFuture.completedFuture(Collections.emptyMap());
        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<RssFeed> collection = database.getCollection(FEEDS_COLLECTION, RssFeed.class);
            try {
                InsertManyResult result = collection.insertMany(Arrays.asList(rssFeeds));
                if (result.wasAcknowledged()) {
                    return result.getInsertedIds().entrySet().stream().collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().asObjectId().getValue().toHexString()
                    ));
                } else {
                    return Collections.emptyMap();
                }
            } catch (Exception e) {
                logger.warn("createNewFeeds failed", e);
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Map<Integer, String>> createNewEntries(Entry... feedEntries) {
        if (feedEntries == null || feedEntries.length == 0) return CompletableFuture.completedFuture(Collections.emptyMap());
        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<Entry> collection = database.getCollection(ENTRIES_COLLECTION, Entry.class);
            try {
                InsertManyResult result = collection.insertMany(Arrays.asList(feedEntries));
                if (result.wasAcknowledged()) {
                    return result.getInsertedIds().entrySet().stream().collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().asObjectId().getValue().toHexString()
                    ));
                } else {
                    return Collections.emptyMap();
                }
            } catch (Exception e) {
                logger.warn("createNewEntries failed", e);
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public void createIndexes() {
        MongoCollection<RssUser> users = database.getCollection(USER_COLLECTION, RssUser.class);
        users.createIndex(Indexes.ascending("username"), new IndexOptions().unique(true));

        MongoCollection<RssFeed> feeds = database.getCollection(FEEDS_COLLECTION, RssFeed.class);
        feeds.createIndex(Indexes.ascending("feedURL"), new IndexOptions().unique(true));

        MongoCollection<Entry> entries = database.getCollection(ENTRIES_COLLECTION, Entry.class);
        entries.createIndex(Indexes.descending("feedId", "pubDate"));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "databaseName='" + databaseName + '\'' + '}';
    }

    /**
     * Shutdown the internal executor used for blocking operations. Safe to call multiple times.
     */
    public void shutdown() {
        try {
            executor.shutdownNow();
        } catch (Exception e) {
            logger.warn("Failed to shutdown executor", e);
        }
    }
}

