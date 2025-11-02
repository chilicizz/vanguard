package com.cyrilng.vanguard.rss.domain;

import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonRepresentation;

import java.util.List;

/**
 *
 * @param userId
 * @param username
 */
public record RssUser(@BsonId()
                      @BsonRepresentation(BsonType.OBJECT_ID) String userId,
                      String username,
                      List<String> rssFeedUrls
) {
}
