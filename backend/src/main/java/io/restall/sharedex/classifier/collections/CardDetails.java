package io.restall.sharedex.classifier.collections;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CardDetails(
        String expansion,
        @JsonProperty("card_id") String cardId,
        String hp,
        String energy,
        String name,
        @JsonProperty("card_type") String cardType,
        @JsonProperty("evolution_type") String evolutionType,
        List<AttackDetails> attacks,
        AbilityDetails ability,
        String rarity,
        String weakness,
        String retreat,
        @JsonProperty("fullart") boolean fullArt,
        boolean ex,
        boolean baby,
        @JsonProperty("alternate_versions") List<Integer> alternateVersions,
        String artist,
        @JsonProperty("internal_id") int internalId
) {
}
