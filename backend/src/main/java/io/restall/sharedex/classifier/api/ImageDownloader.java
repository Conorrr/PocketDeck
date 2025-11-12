package io.restall.sharedex.classifier.api;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

@Slf4j
public class ImageDownloader {

    private static final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/webp"
    );

    private static final long MAX_SIZE = 5 * 1024 * 1024;

    @SneakyThrows
    public BufferedInputStream downloadFile(String uri) {
        var headRequest = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();

        var headResponse = client.send(headRequest, HttpResponse.BodyHandlers.discarding());

        String contentType = headResponse.headers().firstValue("content-type").orElse("");
        long contentLength = headResponse.headers()
                .firstValueAsLong("content-length")
                .orElse(-1);

        if (ALLOWED_TYPES.stream().noneMatch(contentType::startsWith)) {
            log.warn("Disallowed content type: {} for uri: {}", contentType, uri);
            return null;
        }

        if (contentLength > MAX_SIZE) {
            log.warn("File too large: {}bytes for uri: {}", contentType, uri);
            return null;
        }

        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .timeout(Duration.ofSeconds(2))
                .build();

        var getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofInputStream());

        if (getResponse.statusCode() != 200) {
            log.warn("Unexpected response code: {} for uri: {}", getResponse.statusCode(), uri);
            return null;
        }

        return new BufferedInputStream(getResponse.body());
    }

}
