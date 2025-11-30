package com.cyrilng.vanguard;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class RomeHttpClientTest {
    private static final Logger logger = LoggerFactory.getLogger(RomeHttpClientTest.class);
    public static final String URL = "https://www.info.gov.hk/gia/rss/general_en.xml";
    public static final String ARS_URL = "https://arstechnica.com/feed/";

    @Test
    public void testReadUrl() {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ARS_URL)).build();

            CompletableFuture<SyndFeed> future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                    .thenApply(HttpResponse::body)
                    .thenApply(inputStream -> {
                        try {
                            return new SyndFeedInput().build(new XmlReader(inputStream));
                        } catch (FeedException | IOException e) {
                            throw new CompletionException(e);
                        }
                    }).exceptionally(throwable -> {
                        logger.error(throwable.getMessage(), throwable);
                        return null;
                    });
            Awaitility.await().atMost(5, TimeUnit.SECONDS).until(future::isDone);

            SyndFeed feed = future.get();
            assertNotNull(feed);
            assertNotNull(feed.getTitle());
            assertNotNull(feed.getDescription());
            assertNotNull(feed.getLink());
            logger.info("Title: {}", feed.getTitle());
            logger.info("Description: {}", feed.getDescription());
            logger.info("Link: {}", feed.getLink());
            logger.info("URI: {}", feed.getUri());

            List<SyndEntry> entries = feed.getEntries();
            for (SyndEntry entry : entries) {
                logger.info("Title: {}", entry.getTitle());
                logger.info("Link: {}", entry.getLink());
                logger.info("Published Date: {}", entry.getPublishedDate());
                if (entry.getDescription() != null) {
                    logger.info("Description: {}", entry.getDescription().getValue());
                }
            }


            HttpRequest failRequest = HttpRequest.newBuilder().uri(URI.create("https://null.cyrilng.com")).build();
            assertThrows(Exception.class, () -> httpClient.send(failRequest, HttpResponse.BodyHandlers.ofInputStream()));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
