package com.cyrilng.vanguard.rss;

import com.rometools.opml.feed.opml.Opml;
import com.rometools.opml.feed.opml.Outline;

import java.util.ArrayList;
import java.util.List;

/**
 * Extract the feeds from the opml file
 */
public class OPMLProcessor {

    private static void extractFeeds(Outline outline, List<OpmlEntry> output) {
        if ("rss".equals(outline.getType())) {
            output.add(new OpmlEntry(outline.getText(), outline.getXmlUrl()));
        } else if (outline.getChildren() != null && !outline.getChildren().isEmpty()) {
            outline.getChildren().forEach(child -> extractFeeds(child, output));
        }
    }

    public static List<OpmlEntry> extractFeeds(Opml opml) {
        List<OpmlEntry> rssFeeds = new ArrayList<>();
        opml.getOutlines().forEach(outline -> extractFeeds(outline, rssFeeds));
        return rssFeeds;
    }

    public record OpmlEntry(String text, String feedUrl) {
    }
}
