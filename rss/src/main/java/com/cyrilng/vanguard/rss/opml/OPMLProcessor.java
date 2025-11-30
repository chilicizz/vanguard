package com.cyrilng.vanguard.rss.opml;

import com.rometools.opml.feed.opml.Opml;
import com.rometools.opml.feed.opml.Outline;
import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.WireFeedInput;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Extract the feeds from the opml file
 */
public class OPMLProcessor {

    public static List<OpmlEntry> parseOpml(String opmlFeed) throws FeedException {
        return parseOpml(new ByteArrayInputStream(opmlFeed.getBytes(StandardCharsets.UTF_8)));
    }

    public static List<OpmlEntry> parseOpml(InputStream inputStream) throws FeedException {
        WireFeed feed = new WireFeedInput().build(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        if (feed instanceof Opml opml) {
            return extractFeeds(opml);
        } else {
            throw new FeedException("Feed was not recognised as OPML");
        }
    }

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

}
