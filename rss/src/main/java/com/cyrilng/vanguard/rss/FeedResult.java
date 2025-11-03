package com.cyrilng.vanguard.rss;

import com.cyrilng.vanguard.rss.domain.Entry;
import com.cyrilng.vanguard.rss.domain.RssFeed;

public record FeedResult(RssFeed feed,
                         Entry[] itemList) {
}
