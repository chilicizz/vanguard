package com.cyrilng.vanguard.rss;

import com.cyrilng.vanguard.rss.domain.FeedEntry;
import com.cyrilng.vanguard.rss.domain.RssFeed;

public record FeedResult(RssFeed feed,
                         FeedEntry[] itemList) {
}
