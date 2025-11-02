package com.cyrilng.vanguard.rss.domain;

import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonRepresentation;

import java.util.Date;
import java.util.List;

/**
 *
 * @param id
 * @param feedId      identifier for the originating feed
 * @param title
 * @param description
 * @param authors
 * @param pubDate     indexed in descending ie -1 most recent first
 * @param link
 * @param content
 * @param categories
 * @param imageUrl
 */
public record FeedEntry(
        @BsonId()
        @BsonRepresentation(BsonType.OBJECT_ID) String id,
        String feedId,
        String title,
        String description,
        List<String> authors,
        Date pubDate,
        String link,
        String content,
        List<String> categories,
        String imageUrl
) {

}
