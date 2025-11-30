package com.cyrilng.vanguard;

import com.cyrilng.vanguard.rss.opml.OPMLProcessor;
import com.cyrilng.vanguard.rss.RssFeedProcessor;
import com.cyrilng.vanguard.rss.domain.Entry;
import com.cyrilng.vanguard.rss.opml.OpmlEntry;
import com.rometools.opml.feed.opml.Opml;
import com.rometools.opml.feed.opml.Outline;
import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndImage;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.WireFeedInput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RomeParseTest {
    private static final Logger logger = LoggerFactory.getLogger(RomeParseTest.class);

    static SyndFeed arsFeed;
    static SyndFeed hkoWarnings;
    static WireFeed parsedOpml;

    @BeforeAll
    public static void setUp() throws IOException, FeedException {
        try (
                InputStream fileInputStreamARS = RomeParseTest.class.getClassLoader().getResourceAsStream("rss/ars_rss.xml");
                InputStream fileInputStreamHKO = RomeParseTest.class.getClassLoader().getResourceAsStream("rss/hko_warnings.xml");
                InputStream opmlInputStream = RomeParseTest.class.getClassLoader().getResourceAsStream("opml/feedly-2025-11-02.opml")
        ) {
            arsFeed = new SyndFeedInput().build(new InputStreamReader(fileInputStreamARS, StandardCharsets.UTF_8));
            hkoWarnings = new SyndFeedInput().build(new InputStreamReader(fileInputStreamHKO, StandardCharsets.UTF_8));
            parsedOpml = new WireFeedInput().build(new InputStreamReader(opmlInputStream, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void parseArsXml() {
        assertNotNull(arsFeed);
        assertEquals("https://arstechnica.com", arsFeed.getLink());
        assertEquals("Ars Technica - All content", arsFeed.getTitle());
        assertEquals("All Ars Technica stories", arsFeed.getDescription());
        SyndImage image = arsFeed.getImage();
        assertNotNull(image);
        System.out.println(hkoWarnings.getTitle());
    }

    @Test
    public void testParseFeed() {
        List<SyndEntry> entryList = arsFeed.getEntries();
        Entry entry = RssFeedProcessor.processItem(entryList.getFirst());
        assertNotNull(entry);
    }

    @Test
    public void handleOPML() throws FeedException {
        assertNotNull(parsedOpml);
        assertInstanceOf(Opml.class, parsedOpml);
        Opml opml = (Opml) parsedOpml;
        List<Outline> outlines = opml.getOutlines();
        assertNotNull(outlines);
        assertNotNull(outlines.getFirst());
        List<Outline> feeds = outlines.getFirst().getChildren();
        assertNotNull(feeds.getFirst());
        String type = feeds.getFirst().getType();
        assertEquals("rss", type);

        List<OpmlEntry> extracted = OPMLProcessor.extractFeeds(opml);
        assertNotNull(extracted);
        assertFalse(extracted.isEmpty());

        List<OpmlEntry> entryList = OPMLProcessor.parseOpml(RomeParseTest.class.getClassLoader().getResourceAsStream("opml/feedly-2025-11-02.opml"));
        assertNotNull(entryList);
        assertFalse(entryList.isEmpty());
    }

}
