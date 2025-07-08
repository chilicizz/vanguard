package com.cyrilng.vanguard.rss;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

class RSSChannelHandlerTest {
    private static final Logger logger = LoggerFactory.getLogger(RSSChannelHandlerTest.class);

    static String arsXml;
    static SAXParserFactory parserFactory;
    SAXParser saxParser;
    RSSChannelHandler handler;

    @BeforeAll
    public static void setUp() throws IOException {
        try (InputStream fileInputStream = RSSChannelHandlerTest.class.getClassLoader().getResourceAsStream("rss/ars_rss.xml")) {
            arsXml = new String(fileInputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        parserFactory = SAXParserFactory.newInstance();

    }

    @BeforeEach
    public void setUpParser() throws ParserConfigurationException, SAXException {
        saxParser = parserFactory.newSAXParser();
    }

    @Test
    void startDocument() throws IOException, SAXException {
        handler = new RSSChannelHandler(rssChannel -> {
            logger.info(String.valueOf(rssChannel));
        });
        saxParser.parse(new InputSource(new StringReader(arsXml)), handler);
    }

}