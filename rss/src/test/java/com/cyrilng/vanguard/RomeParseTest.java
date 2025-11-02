package com.cyrilng.vanguard;

import com.cyrilng.vanguard.rss.RssFeedProcessor;
import com.cyrilng.vanguard.rss.domain.FeedEntry;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndImage;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RomeParseTest {
    private static final Logger logger = LoggerFactory.getLogger(RomeParseTest.class);

    static SyndFeed arsFeed;
    static SyndFeed hkoWarnings;

    @BeforeAll
    public static void setUp() throws IOException, FeedException {
        try (
                InputStream fileInputStreamARS = RomeParseTest.class.getClassLoader().getResourceAsStream("rss/ars_rss.xml");
                InputStream fileInputStreamHKO = RomeParseTest.class.getClassLoader().getResourceAsStream("rss/hko_warnings.xml")
        ) {
            arsFeed = new SyndFeedInput().build(new InputStreamReader(fileInputStreamARS, StandardCharsets.UTF_8));
            hkoWarnings = new SyndFeedInput().build(new InputStreamReader(fileInputStreamHKO, StandardCharsets.UTF_8));
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
        FeedEntry entry = RssFeedProcessor.processItem(entryList.getFirst());
        assertNotNull(entry);
    }
}
