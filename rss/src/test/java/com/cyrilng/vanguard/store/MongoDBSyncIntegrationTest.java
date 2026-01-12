package com.cyrilng.vanguard.store;

import com.cyrilng.vanguard.rss.domain.RssFeed;
import com.cyrilng.vanguard.rss.mongo.MongoUtils;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

public class MongoDBSyncIntegrationTest {

    public static MongoClient mongoClient;

    @BeforeAll
    public static void setUp() {
        System.out.println("Setting up synchronous MongoDB client...");
        String connectionString = System.getenv(MongoUtils.MONGO_CONNECTION_STRING);
        assertNotNull(connectionString, "MONGO_CONNECTION_STRING environment variable is not set");
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .serverApi(serverApi)
                .build();
        mongoClient = MongoClients.create(settings);
    }

    @AfterAll
    public static void tearDown() {
        System.out.println("Closing synchronous MongoDB client...");
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Test
    public void mongoDBConnectionTest() {
        MongoDatabase database = mongoClient.getDatabase(MongoUtils.ADMIN_DB);
        Document result = database.runCommand(new Document("ping", 1));
        assertNotNull(result);
        Object okObj = result.get("ok");
        assertNotNull(okObj);
        // ok may be Double or Integer depending on server/driver
        int okValue = (okObj instanceof Number) ? ((Number) okObj).intValue() : 0;
        assertEquals(1, okValue);
        System.out.println("Pinged your deployment. You successfully connected to MongoDB!");
    }

    @Test
    public void mongoDBCrudTest() {
        MongoDatabase database = mongoClient.getDatabase(MongoUtils.TEMP_DB);
        Document doc = new Document("name", "testDocument").append("value", 123);

        // Insert
        InsertOneResult insertResult = database.getCollection("test").insertOne(doc);
        assertTrue(insertResult.wasAcknowledged());

        // Verify inserted
        Document found = database.getCollection("test").find(doc).first();
        assertNotNull(found);
        assertEquals(123, found.getInteger("value").intValue());
        assertEquals("testDocument", found.getString("name"));

        // Update
        UpdateResult updateResult = database.getCollection("test")
                .updateOne(doc, new Document("$set", new Document("value", 456)));
        assertEquals(1, updateResult.getModifiedCount());

        // Clean up
        DeleteResult deleteResult = database.getCollection("test").deleteMany(new Document("value", 456));
        assertTrue(deleteResult.getDeletedCount() > 0);
    }

    @Test
    public void testUsingRecord() {
        MongoDatabase database = mongoClient.getDatabase(MongoUtils.TEMP_DB);
        MongoCollection<RssFeed> collection = database.getCollection("test-feed", RssFeed.class);
        RssFeed rssFeed = new RssFeed(URI.create("https://dummy.com/feed"), "Test Title", "Test Description", "linkString", "https://dummy.com/feed.png");

        InsertOneResult insertOneResult = collection.insertOne(rssFeed);
        assertTrue(insertOneResult.wasAcknowledged());

        // Verify that the document was inserted
        RssFeed found = collection.find(new Document().append("feedURL", "https://dummy.com/feed")).first();
        assertNotNull(found);
        assertEquals("Test Title", found.title());
        assertEquals("Test Description", found.description());

        // Try update
        UpdateResult updateResult = collection.updateOne(new Document().append("feedURL", "https://dummy.com/feed"), new Document("$set", new Document("feedURL", "http://dummy.com/feed")));
        assertEquals(1, updateResult.getModifiedCount());

        // Clean up the inserted document
        DeleteResult deleteResult = collection.deleteMany(new Document().append("title", "Test Title"));
        assertTrue(deleteResult.getDeletedCount() > 0);
    }
}

