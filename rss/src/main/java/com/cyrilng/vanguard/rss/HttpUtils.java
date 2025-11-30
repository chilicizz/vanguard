package com.cyrilng.vanguard.rss;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class HttpUtils {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    public static CompletableFuture<SyndFeed> fetchFeed(HttpClient httpClient, URI uri) {
        try {
            logger.info("Fetching feed: {}", uri);
            HttpRequest request = HttpRequest.newBuilder().uri(uri).build();
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
            return future;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

}
