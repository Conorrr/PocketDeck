package io.restall.sharedex.classifier.collections;

import java.util.List;

public record UpdatedCardDetails(
        List<String> expansions,
        List<String> cardIds,
        String hp,
        String energy,
        String name,
        String cardType,
        String evolutionType,
        List<AttackDetails> attacks,
        AbilityDetails ability,
        String rarity,
        String weakness,
        String retreat,
        boolean fullArt,
        boolean ex,
        boolean baby,
        List<String> alternateVersions,
        String artist
) { }
