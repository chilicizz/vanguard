package com.cyrilng.vanguard.rss;

import com.cyrilng.vanguard.rss.domain.RssFeed;
import com.cyrilng.vanguard.rss.domain.FeedEntry;
import com.cyrilng.vanguard.rss.domain.RssUser;

public interface RSSFeedClient {

    RssUser fetchUser(String sessionId);

    RssUser updateUser(RssUser user);

    RssFeed fetchRssFeed(String feedUrl);

    FeedEntry[] fetchFeedEntries(RssFeed rssFeed);
}
