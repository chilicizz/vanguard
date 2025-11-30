package com.cyrilng.vanguard.rss.mongo;

import com.cyrilng.vanguard.rss.domain.Entry;
import com.cyrilng.vanguard.rss.domain.RssFeed;
import com.cyrilng.vanguard.rss.domain.RssUser;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface StorageInterface {
    CompletableFuture<RssUser> fetchUserById(String userId);

    CompletableFuture<RssUser> fetchUserByUsername(String username);

    CompletableFuture<String> createNewUser(String username);

    CompletableFuture<Boolean> updateUserRssFeeds(String userId, List<String> feedUrls);

    CompletableFuture<Boolean> deleteUser(String userId);

    CompletableFuture<List<RssFeed>> fetchFeeds(String... feedUrls);

    CompletableFuture<Map<Integer, String>> createNewFeeds(RssFeed... rssFeeds);

    CompletableFuture<Map<Integer, String>> createNewEntries(Entry... feedEntries);

    void createIndexes();
}
