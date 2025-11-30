package com.cyrilng.vanguard.rss;

import com.cyrilng.vanguard.rss.domain.Entry;
import com.cyrilng.vanguard.rss.domain.EntryBuilder;
import com.cyrilng.vanguard.rss.domain.RssFeed;
import com.cyrilng.vanguard.rss.mongo.StorageInterface;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndPerson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * URL -> returns the response
 */
public class RssFeedProcessor {
    private static final Logger logger = LoggerFactory.getLogger(RssFeedProcessor.class);

    private final StorageInterface storageInterface;
    private final HttpClient httpClient;

    public RssFeedProcessor(StorageInterface storageInterface, HttpClient httpClient) {
        this.storageInterface = storageInterface;
        this.httpClient = httpClient;
    }

    public static FeedResult processSyndFeed(SyndFeed syndFeed) {
        URI originalUrl = null;
        RssFeed rssFeed = new RssFeed(originalUrl, syndFeed.getTitle(), syndFeed.getDescription(), syndFeed.getLink(), syndFeed.getImage() != null ? syndFeed.getImage().getUrl() : null);
        List<Entry> entries = new ArrayList<>();
        List<SyndEntry> entryList = syndFeed.getEntries();
        for (SyndEntry entry : entryList) {
            try {
                Entry processedEntry = processItem(entry);
                entries.add(processedEntry);
            } catch (Exception e) {
                logger.warn("Failed to process feed entry {}", entry, e);
            }
        }
        return new FeedResult(null, entries.toArray(Entry[]::new));
    }

    public static Entry processItem(SyndEntry entry) {
        EntryBuilder builder = new EntryBuilder();

        String uri = entry.getUri();
        List<String> authors = new ArrayList<>();
        Optional.ofNullable(entry.getAuthor()).ifPresent(author -> authors.add(strip(author)));
        if (entry.getAuthors() != null) {
            for (SyndPerson syndPerson : entry.getAuthors()) {
                Optional.ofNullable(syndPerson.getName()).ifPresent(name -> authors.add(strip(name)));
            }
        }
        List<String> categories = new ArrayList<>();
        if (entry.getCategories() != null) {
            entry.getCategories().forEach(category -> categories.add(strip(category.getName())));
        }
        if (entry.getContents() != null) {
            List<SyndContent> contents = entry.getContents();
            for (SyndContent content : contents) {
                String type = content.getType(); //html?
                String value = strip(content.getValue());

                builder.setContent(value);
            }
        }
        String description = strip(entry.getDescription().getValue());

        builder.setLink(uri);
        builder.setCategories(categories);
        builder.setAuthors(authors);
        builder.setDescription(description);
        Date date = entry.getPublishedDate();
        builder.setPubDate(date);
        builder.setTitle(strip(entry.getTitle()));
        return builder.createRssFeedEntry();
    }

    private static String strip(String string) {
        return string != null ? string.strip() : null;
    }

    public FeedResult handleUrl(String url) {
        try {
            URI uri = URI.create(url);
            CompletableFuture<FeedResult> feedCompletableFuture = HttpUtils.fetchFeed(httpClient, uri)
                    .thenApply(RssFeedProcessor::processSyndFeed);
            return feedCompletableFuture.get();
        } catch (Exception e) {
            logger.warn("Failed to handle: {}", url);
            return null;
        }
    }
}
