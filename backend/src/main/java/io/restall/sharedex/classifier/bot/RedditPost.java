package io.restall.sharedex.classifier.bot;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

public record RedditPost(
        String id,
        String title,
        String selftext,
        @JsonProperty("media_metadata") Map<String, RedditGalleryMetadata> mediaMetadata,
        String url,
        @JsonProperty("created_utc") Instant created
) {
}
