package com.cyrilng.datex.rss;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.cyrilng.datex.rss.RSSChannelHandler.attributeString;

public class RSSItemHandler extends DefaultHandler {
    private static final Logger logger = LoggerFactory.getLogger(RSSItemHandler.class);

    private final StringBuilder elementValue;
    private final Consumer<RSSItem> feedConsumer;
    private RSSItemBuilder itemBuilder;

    public RSSItemHandler(Consumer<RSSItem> feedConsumer) {
        this.feedConsumer = feedConsumer;
        this.elementValue = new StringBuilder();
        this.itemBuilder = new RSSItemBuilder();
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        elementValue.append(ch, start, length);
    }

    @Override
    public void startDocument() throws SAXException {
        logger.trace("startDocument()");
    }

    @Override
    public void startElement(String uri, String lName, String qName, Attributes attr) {
        logger.trace("startElement({}, {}, {}, {})", uri, lName, qName, attributeString(attr));
        elementValue.setLength(0);
        switch (qName) {
            case "item" -> {
                this.itemBuilder = new RSSItemBuilder();
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        String value = elementValue.toString().strip();
        logger.trace("endElement({}, {}, {}) text: '{}'", uri, localName, qName, value);

        switch (qName) {
            case "item" -> {
                RSSItem rssItem = itemBuilder.createRSSItem();
                Optional.ofNullable(feedConsumer).ifPresent(rssFeedConsumer -> rssFeedConsumer.accept(rssItem));
            }
            case "title" -> itemBuilder.title = value;
            case "link" -> itemBuilder.link = value;
            case "description" -> itemBuilder.description = value;
            case "category" -> {
                if (!value.isBlank()) {
                    itemBuilder.addCategory(value);
                }
            }
            case "guid" -> itemBuilder.guid = value;
            case "pubDate" -> {
                OffsetDateTime offsetDateTime = OffsetDateTime.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(value));
                itemBuilder.pubDate = offsetDateTime.toZonedDateTime();
            }
        }
        if (qName.contains("content")) {
            itemBuilder.content = value;
        }
    }

    @Override
    public void endDocument() {
        logger.trace("endDocument()");
    }

    public static class RSSItemBuilder {
        private final List<String> categories;
        private String title;
        private String link;
        private String description;
        private String guid;
        private ZonedDateTime pubDate;
        private String content;

        public RSSItemBuilder() {
            categories = new ArrayList<>();
        }

        public RSSItemBuilder addCategory(String category) {
            this.categories.add(category);
            return this;
        }

        public RSSItem createRSSItem() {
            return new RSSItem(title, link, description, categories.toArray(String[]::new), guid, pubDate, content);
        }
    }
}
