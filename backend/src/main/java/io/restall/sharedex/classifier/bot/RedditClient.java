package io.restall.sharedex.classifier.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@AllArgsConstructor
public class RedditClient {

    private final ObjectMapper om;

    private static final String URL_PATTERN = "https://www.reddit.com/r/%s/new.json";
    private static final String USER_AGENT = "";
    private final HttpClient client = HttpClient.newHttpClient();

    @SneakyThrows
    public List<RedditPost> getLatest(String subreddit) {
        var url = URL_PATTERN.formatted(subreddit);

        var req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "poke deck updater (by u/conorrr)")
                .GET()
                .build();

        var respIs = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        var apiResponse = om.readValue(respIs.body(), RedditApiResponse.class);

        return apiResponse.data()
                .children()
                .stream()
                .map(RedditApiChildren::data)
                .toList();
    }

}
