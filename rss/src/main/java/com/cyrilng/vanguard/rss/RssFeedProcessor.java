package com.cyrilng.vanguard.rss;

import com.cyrilng.vanguard.rss.domain.Entry;
import com.cyrilng.vanguard.rss.domain.EntryBuilder;
import com.cyrilng.vanguard.rss.domain.RssFeed;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndPerson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class RssFeedProcessor {
    private final Consumer<RssFeed> feedConsumer;
    private final Consumer<Entry[]> itemConsumer;

    public RssFeedProcessor(Consumer<RssFeed> feedConsumer, Consumer<Entry[]> itemConsumer) {
        this.feedConsumer = feedConsumer;
        this.itemConsumer = itemConsumer;
    }

    public static FeedResult processSyndFeed(SyndFeed syndFeed) {
        List<SyndEntry> entryList = syndFeed.getEntries();
        return new FeedResult(null, null);
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
}
