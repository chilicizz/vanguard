package com.cyrilng.vanguard.rss.mongo;

import com.cyrilng.vanguard.rss.domain.RssUser;
import com.mongodb.reactivestreams.client.MongoClient;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.cyrilng.vanguard.rss.mongo.AsyncUtils.createClient;
import static org.junit.jupiter.api.Assertions.*;

class MongoInterfaceIntegrationTest {

    public static MongoStorage mongoInterface;

    @BeforeAll
    public static void setUpClient() {
        String connectionString = System.getenv(Constants.MONGO_CONNECTION_STRING);
        org.junit.jupiter.api.Assumptions.assumeTrue(connectionString != null && !connectionString.isEmpty(), "MONGO_CONNECTION_STRING environment variable is not set");
        com.mongodb.reactivestreams.client.MongoClient mongoClient = createClient(connectionString);
        try {
            AsyncUtils.singleResultFrom(mongoClient.getDatabase(Constants.ADMIN_DB).runCommand(new org.bson.Document("ping", 1))).join();
        } catch (Throwable t) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Skipping Mongo tests: " + t.getMessage());
        }
        AsyncUtils.singleResultFrom(mongoClient.getDatabase(Constants.TEST_DB).drop()).join();
        mongoInterface = new MongoStorage(mongoClient, Constants.TEST_DB);
    }

    @Test
    void createNewUser() throws ExecutionException, InterruptedException {
        // check does not exist
        var notExist = mongoInterface.fetchUserByUsername("testuser");
        Awaitility.await().until(notExist::isDone);
        assertNull(notExist.get());

        // CREATE
        var insertResult = mongoInterface.createNewUser("testuser");
        Awaitility.await().until(insertResult::isDone);
        String id = insertResult.get();

        // READ
        CompletableFuture<RssUser> result = mongoInterface.fetchUserById(id);
        Awaitility.await().until(result::isDone);
        RssUser rssUser = result.get();
        assertNotNull(rssUser);
        assertEquals("testuser", rssUser.username());
        assertEquals(id, rssUser.userId());
        assertNull(rssUser.rssFeedUrls());

        // UPDATE
        CompletableFuture<Boolean> updateResult = mongoInterface.updateUserRssFeeds(id, List.of("http://test.com/feed"));
        Awaitility.await().until(updateResult::isDone);
        assertTrue(updateResult.get());

        CompletableFuture<RssUser> postUpdateRes = mongoInterface.fetchUserById(id);
        Awaitility.await().until(postUpdateRes::isDone);
        rssUser = postUpdateRes.get();
        assertNotNull(rssUser);
        assertEquals("testuser", rssUser.username());
        assertEquals(id, rssUser.userId());
        assertNotNull(rssUser.rssFeedUrls());
        assertEquals("http://test.com/feed", rssUser.rssFeedUrls().getFirst());

        // DELETE
        CompletableFuture<Boolean> delResult = mongoInterface.deleteUser(id);
        Awaitility.await().until(delResult::isDone);
        assertTrue(delResult.get());

        // CHECK DELETED
        CompletableFuture<RssUser> fetchTwo = mongoInterface.fetchUserById(id);
        Awaitility.await().until(fetchTwo::isDone);
        rssUser = fetchTwo.get();
        assertNull(rssUser);
    }

    @Test
    void fetchFeeds() {
    }
}