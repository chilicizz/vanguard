package com.cyrilng.vanguard.rss.domain;

import java.util.Date;
import java.util.List;

public class EntryBuilder {
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

    public EntryBuilder setId(String id) {
        this.id = id;
        return this;
    }

    public EntryBuilder setFeedId(String feedId) {
        this.feedId = feedId;
        return this;
    }

    public EntryBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public EntryBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public EntryBuilder setAuthors(List<String> authors) {
        this.authors = authors;
        return this;
    }

    public EntryBuilder setPubDate(Date pubDate) {
        this.pubDate = pubDate;
        return this;
    }

    public EntryBuilder setLink(String link) {
        this.link = link;
        return this;
    }

    public EntryBuilder setContent(String content) {
        this.content = content;
        return this;
    }

    public EntryBuilder setCategories(List<String> categories) {
        this.categories = categories;
        return this;
    }

    public EntryBuilder setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    public Entry createRssFeedEntry() {
        return new Entry(id, feedId, title, description, authors, pubDate, link, content, categories, imageUrl);
    }
}