package com.cyrilng.vanguard.rss.domain;

import java.util.Date;
import java.util.List;

public class FeedEntryBuilder {
    private String id;
    private String feedId;
    private String title;
    private String description;
    private List<String> authors;
    private Date pubDate;
    private String link;
    private String content;
    private List<String> categories;
    private String imageUrl;

    public FeedEntryBuilder setId(String id) {
        this.id = id;
        return this;
    }

    public FeedEntryBuilder setFeedId(String feedId) {
        this.feedId = feedId;
        return this;
    }

    public FeedEntryBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public FeedEntryBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public FeedEntryBuilder setAuthors(List<String> authors) {
        this.authors = authors;
        return this;
    }

    public FeedEntryBuilder setPubDate(Date pubDate) {
        this.pubDate = pubDate;
        return this;
    }

    public FeedEntryBuilder setLink(String link) {
        this.link = link;
        return this;
    }

    public FeedEntryBuilder setContent(String content) {
        this.content = content;
        return this;
    }

    public FeedEntryBuilder setCategories(List<String> categories) {
        this.categories = categories;
        return this;
    }

    public FeedEntryBuilder setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    public FeedEntry createRssFeedEntry() {
        return new FeedEntry(id, feedId, title, description, authors, pubDate, link, content, categories, imageUrl);
    }
}