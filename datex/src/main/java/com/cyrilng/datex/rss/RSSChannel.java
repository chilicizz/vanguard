package com.cyrilng.datex.rss;

import java.time.ZonedDateTime;

public record RSSChannel(
        String title,
        String link,
        String description,
        ZonedDateTime lastBuildDate,
        String language,
        RSSItem[] items
) {
}
