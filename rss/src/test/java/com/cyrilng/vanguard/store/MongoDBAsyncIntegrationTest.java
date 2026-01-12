package com.cyrilng.vanguard.store;

import com.cyrilng.vanguard.rss.domain.RssFeed;
import com.cyrilng.vanguard.rss.mongo.Constants;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.net.URI;

public class MongoDBAsyncIntegrationTest {

    public static MongoClient mongoClient;

    @BeforeAll
    public static void setUp() {
        // This is a placeholder for an actual MongoDB connection test
        System.out.println("Testing MongoDB connection...");
        // Add your MongoDB connection test code here
        // For example, you could check if you can retrieve a collection or document
        String connectionString = System.getenv(Constants.MONGO_CONNECTION_STRING);
        assert connectionString != null && !connectionString.isEmpty() : "MONGO_CONNECTION_STRING environment variable is not set";
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
        // This is a placeholder for an actual MongoDB connection test
        System.out.println("Closing MongoDB connection...");
        // Add your MongoDB connection close code here
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Test
    public void mongoDBConnectionTest() {
        // Send a ping to confirm a successful connection
        MongoDatabase database = mongoClient.getDatabase(Constants.ADMIN_DB);
        StepVerifier.create(database.runCommand(new Document("ping", 1))).expectNextMatches(
                result -> result.getInteger("ok") == 1
        ).expectComplete().verify();
        System.out.println("Pinged your deployment. You successfully connected to MongoDB!");
    }

    @Test
    public void mongoDBCrudTest() {
        // https://www.baeldung.com/reactive-streams-step-verifier-test-publisher
        MongoDatabase database = mongoClient.getDatabase(Constants.TEMP_DB);
        Document doc = new Document("name", "testDocument").append("value", 123);
        StepVerifier.create(database.getCollection("test").insertOne(doc))
                .expectNextMatches(InsertOneResult::wasAcknowledged)
                .expectComplete()
                .verify();

        // Verify that the document was inserted
        StepVerifier.create(database.getCollection("test").find(doc))
                .expectNextMatches(document -> document.getInteger("value").equals(123) &&
                        document.getString("name").equals("testDocument"))
                .expectComplete()
                .verify();

        // Try update
        StepVerifier.create(database.getCollection("test")
                        .updateOne(doc, new Document("$set", new Document("value", 456))))
                .expectNextMatches(result -> result.getModifiedCount() == 1)
                .expectComplete()
                .verify();

        // Clean up the inserted document
        StepVerifier.create(database.getCollection("test").deleteMany(new Document("value", 456)))
                .expectNextMatches(result -> result.getDeletedCount() > 0)
                .expectComplete()
                .verify();
    }

    @Test
    public void testUsingRecord() {
        MongoDatabase database = mongoClient.getDatabase(Constants.TEMP_DB);
        MongoCollection<RssFeed> collection = database.getCollection("test-feed", RssFeed.class);
        RssFeed rssFeed = new RssFeed(URI.create("https://dummy.com/feed"), "Test Title", "Test Description", "linkString", "https://dummy.com/feed.png");
        StepVerifier.create(collection.insertOne(rssFeed))
                .expectNextMatches(InsertOneResult::wasAcknowledged)
                .expectComplete()
                .verify();

        // Verify that the document was inserted
        StepVerifier.create(collection.find(new Document().append("feedURL", "https://dummy.com/feed")))
                .expectNextMatches(document -> document.title().equals("Test Title") &&
                        document.description().equals("Test Description"))
                .expectComplete()
                .verify();

        // Try update
        StepVerifier.create(collection.updateOne(new Document().append("feedURL", "https://dummy.com/feed"), new Document("$set", new Document("feedURL", "http://dummy.com/feed"))))
                .expectNextMatches(result -> result.getModifiedCount() == 1)
                .expectComplete()
                .verify();

        // Clean up the inserted document
        StepVerifier.create(collection.deleteMany(new Document().append("title", "Test Title")))
                .expectNextMatches(result -> result.getDeletedCount() > 0)
                .expectComplete()
                .verify();
    }
}
