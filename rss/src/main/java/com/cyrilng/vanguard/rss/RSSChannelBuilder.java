package com.cyrilng.vanguard.rss;

import java.time.ZonedDateTime;

public class RSSChannelBuilder {
    private String title;
    private String link;
    private String description;
    private ZonedDateTime lastBuildDate;
    private String language;
    private RSSItem[] items;

    public RSSChannelBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public RSSChannelBuilder setLink(String link) {
        this.link = link;
        return this;
    }

    public RSSChannelBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public RSSChannelBuilder setLastBuildDate(ZonedDateTime lastBuildDate) {
        this.lastBuildDate = lastBuildDate;
        return this;
    }

    public RSSChannelBuilder setLanguage(String language) {
        this.language = language;
        return this;
    }

    public RSSChannelBuilder setItems(RSSItem[] items) {
        this.items = items;
        return this;
    }

    public RSSChannel createRSSChannel() {
        return new RSSChannel(title, link, description, lastBuildDate, language, items);
    }
}