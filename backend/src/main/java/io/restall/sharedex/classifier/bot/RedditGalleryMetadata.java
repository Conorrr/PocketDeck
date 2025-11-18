package io.restall.sharedex.classifier.bot;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RedditGalleryMetadata(
        @JsonProperty("e") String type,
        @JsonProperty("s") RedditGalleryImageData sourceData
) {
}
