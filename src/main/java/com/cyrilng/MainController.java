package com.cyrilng;

import com.cyrilng.vanguard.rss.domain.RssFeed;
import com.cyrilng.vanguard.rss.domain.RssUser;
import com.cyrilng.vanguard.rss.mongo.StorageInterface;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.http.cookie.Cookies;
import io.micronaut.views.ModelAndView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static io.micronaut.http.MediaType.*;

@Controller
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private final HttpClient httpClient;
    private final StorageInterface storage;

    public MainController(StorageInterface storage) {
        this.storage = storage;
        this.httpClient = HttpClient.newBuilder().build();
    }

    @Get(value = "/", produces = TEXT_HTML)
    public ModelAndView index(HttpRequest<Void> request) {
        Cookies cookies = request.getCookies();
        if (cookies.contains("userId")) {
            // fetch user detail
            return new ModelAndView("home", Map.of("userId", cookies.get("userId").getValue()));
        }
        return new ModelAndView("index", Map.of("message", "notLoggedIn"));
    }

    @Get(value = "/feeds", produces = TEXT_HTML)
    public ModelAndView feeds(HttpRequest<Void> request) {
        Cookies cookies = request.getCookies();
        if (cookies.contains("userId")) {
            // fetch user detail
            return new ModelAndView("update_feeds", Map.of("userId", cookies.get("userId").getValue()));
        }
        return new ModelAndView("index", Map.of("message", "notLoggedIn"));
    }

    @Post(value = "/getUser", produces = APPLICATION_JSON)
    public CompletableFuture<RssUser> getUser(HttpRequest<Void> request) {
        Cookies cookies = request.getCookies();
        if (cookies.contains("userId")) {
            // fetch user detail
            CompletableFuture<RssUser> user = storage.fetchUserById(cookies.get("userId").getValue());
            return user;
        }
        return CompletableFuture.failedFuture(new RuntimeException("not logged in"));
    }

    @Post(value = "/getFeed", produces = APPLICATION_JSON)
    public CompletableFuture<List<RssFeed>> getFeeds(@Parameter("feedId") String feedId, HttpRequest<Void> request) {
        Cookies cookies = request.getCookies();
        if (cookies.contains("userId")) {
            CompletableFuture<List<RssFeed>> feeds = storage.fetchFeeds(feedId);
            return feeds;
        }
        return CompletableFuture.failedFuture(new RuntimeException("not logged in"));
    }

    @Get(value = "/view", produces = TEXT_HTML)
    public ModelAndView viewFeed(@Parameter("entryId") String entryId, HttpRequest<Void> request) {
        Cookies cookies = request.getCookies();
        if (cookies.contains("userId")) {
            // fetch entry id
            return new ModelAndView("article", Map.of(
                    "id", entryId,
                    "title", entryId,
                    "description", "description"
            ));
        }
        return new ModelAndView("index", Map.of("message", "notLoggedIn"));
    }

    @Post(value = "/validate/feed", consumes = APPLICATION_FORM_URLENCODED, produces = APPLICATION_JSON)
    public String validateFeed(@Parameter("feedURL") String feedUrl) {
        logger.info("Validating: {}", feedUrl);
        URI feed = URI.create(feedUrl);
        // fetch the feed
        logger.info(String.valueOf(feed));
        java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder(feed).GET().build();
        try {
            java.net.http.HttpResponse<String> response = httpClient.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
            logger.info(response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Post(value = "/register")
    public HttpResponse<String> authenticate(@Parameter("username") String username, @Parameter("password") String password) {
        // Fetch User Information From DB
        return HttpResponse.ok("").cookie(Cookie.of("userId", UUID.randomUUID().toString()));
    }

    @Post(value = "/login")
    public HttpResponse<String> authenticate(@Parameter("username") String username) {
        return HttpResponse.ok("").cookie(Cookie.of("userId", UUID.randomUUID().toString()));
    }

    @Get(value = "/cookie/{value}")
    public String getCookie(@PathVariable("value") String value, HttpRequest<Void> request) {
        Cookies cookies = request.getCookies();
        if (cookies.contains(value)) {
            return cookies.get(value).getValue();
        }
        return value;
    }
}
