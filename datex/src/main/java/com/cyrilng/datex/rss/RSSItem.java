package com.cyrilng.datex.rss;

import java.time.ZonedDateTime;

/**
 * <a href="https://www.rssboard.org/rss-specification#hrelementsOfLtitemgt">...</a>
 *
 * @param title
 * @param link
 * @param description
 * @param categories
 * @param guid
 * @param pubDate
 * @param content
 */
public record RSSItem(
        String title,
        String link,
        String description,
        String[] categories,
        String guid,
        ZonedDateTime pubDate,
        String content
) {
}
