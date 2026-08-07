package com.cyrilng.vanguard.rss.mongo;

import com.cyrilng.vanguard.rss.domain.RssUser;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class MongoInterfaceSyncIntergrationTest {

    public static StorageInterface mongoInterface;

    @BeforeAll
    public static void setUpClient() {
        String connectionString = System.getenv(Constants.MONGO_CONNECTION_STRING);
        assertNotNull(connectionString, "MONGO_CONNECTION_STRING environment variable is not set");
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .serverApi(serverApi)
                .build();
        MongoClient mongoClient = MongoClients.create(settings);

        try {
            Object okObj = mongoClient.getDatabase(Constants.ADMIN_DB).runCommand(new org.bson.Document("ping", 1)).get("ok");
            org.junit.jupiter.api.Assumptions.assumeTrue(okObj != null, "Skipping Mongo tests: ping failed");
        } catch (Throwable t) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Skipping Mongo tests: " + t.getMessage());
        }

        mongoInterface = new MongoStorageSync(mongoClient, Constants.TEST_DB);
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
