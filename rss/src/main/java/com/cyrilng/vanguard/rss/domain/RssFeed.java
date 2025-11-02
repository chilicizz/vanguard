package com.cyrilng.vanguard.rss.domain;

import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonRepresentation;

import java.net.URI;

/**
 *
 * @param id
 * @param feedURL     should be unique and indexed
 * @param title
 * @param description
 * @param link
 * @param imageUrl
 */
public record RssFeed(@BsonId()
                      @BsonRepresentation(BsonType.OBJECT_ID) String id,
                      String feedURL,
                      String title,
                      String description,
                      String link,
                      String imageUrl
) {
    public RssFeed(URI feedURL, String title, String description, String link, String imageURL) {
        this(null, feedURL != null ? feedURL.toString() : null, title, description, link, imageURL);
    }

    public URI getFeedURI() {
        return URI.create(feedURL);
    }
}
