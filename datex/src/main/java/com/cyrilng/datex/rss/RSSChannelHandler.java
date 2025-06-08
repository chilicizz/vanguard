package com.cyrilng.datex.rss;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 *
 */
public class RSSChannelHandler extends DefaultHandler {
    private static final Logger logger = LoggerFactory.getLogger(RSSChannelHandler.class);

    private final StringBuilder elementValue;
    private final Consumer<RSSChannel> feedConsumer;
    private final RSSItemHandler rssItemHandler;
    private final List<RSSItem> items;
    private RSSChannelBuilder channelBuilder;

    public RSSChannelHandler(Consumer<RSSChannel> feedConsumer) {
        this.feedConsumer = feedConsumer;
        this.elementValue = new StringBuilder();
        this.items = new ArrayList<>();
        this.rssItemHandler = new RSSItemHandler(items::add);
    }

    public static String attributeString(Attributes attributes) {
        StringBuilder builder = new StringBuilder("{");
        for (int i = 0; i < attributes.getLength(); i++) {
            builder.append(attributes.getLocalName(i)).append(": ").append(attributes.getValue(i)).append(", ");
        }
        builder.append("}");
        return builder.toString();
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        elementValue.append(ch, start, length);
        rssItemHandler.characters(ch, start, length);
    }

    @Override
    public void startDocument() throws SAXException {
        logger.trace("startDocument()");
        rssItemHandler.startDocument();
        channelBuilder = new RSSChannelBuilder();
    }

    @Override
    public void startElement(String uri, String lName, String qName, Attributes attr) throws SAXException {
        logger.info("startElement({}, {}, {}, {})", uri, lName, qName, attributeString(attr));
        rssItemHandler.startElement(uri, lName, qName, attr);
        // reset the tag value
        elementValue.setLength(0);
        switch (qName) {
            case "channel" -> this.channelBuilder = new RSSChannelBuilder();
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        String value = elementValue.toString().strip();
        logger.info("endElement({}, {}, {}) text: '{}'", uri, localName, qName, value);
        rssItemHandler.endElement(uri, localName, qName);
        switch (qName) {
            case "title" -> channelBuilder.setTitle(value);
            case "link" -> channelBuilder.setLink(value);
            case "description" -> channelBuilder.setDescription(value);
            case "lastBuildDate" -> {
                OffsetDateTime offsetDateTime = OffsetDateTime.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(value));
                channelBuilder.setLastBuildDate(offsetDateTime.toZonedDateTime());
            }
            case "language" -> channelBuilder.setLanguage(value);
        }
    }

    @Override
    public void endDocument() throws SAXException {
        logger.trace("endDocument()");
        rssItemHandler.endDocument();
        channelBuilder.setItems(items.toArray(RSSItem[]::new));
        RSSChannel channel = channelBuilder.createRSSChannel();
        Optional.ofNullable(feedConsumer).ifPresent(rssFeedConsumer -> rssFeedConsumer.accept(channel));
    }
}
