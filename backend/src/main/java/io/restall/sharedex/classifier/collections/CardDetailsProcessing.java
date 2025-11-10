package io.restall.sharedex.classifier.collections;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restall.sharedex.classifier.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.nio.file.Files.newOutputStream;
import static java.util.Collections.emptyList;

public class CardDetailsProcessing {

    private static final ObjectMapper OM = new ObjectMapper();

    private static final Pattern EXPANSION_PATTERN = Pattern.compile("([A-Z][0-9][ab]?|P-A)-[0-9]{1,3}");

    static {
        OM.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static void main(String[] args) throws IOException {
        var config = AppConfig.fromEnv();
        var allCards = OM.readValue(Files.newInputStream(config.rawCardDetailsPath()), new TypeReference<List<CardDetails>>() {
        });

        var altMap = allCards.stream()
                .collect(Collectors.toMap(CardDetails::internalId, CardDetails::cardId, CardDetailsProcessing::compareCardId));

        var idMap = allCards.stream()
                .collect(Collectors.groupingBy(CardDetails::internalId));

        var fixedCards = allCards.stream()
                .map(cardDetails -> fixCardDetails(cardDetails, idMap, altMap))
                .toList();

        OM.writeValue(newOutputStream(config.allCardsPath()), fixedCards);
        System.out.printf("Updated Card JSON file written: %s \n", config.allCardsPath().toAbsolutePath().normalize());
    }

    private static UpdatedCardDetails fixCardDetails(CardDetails original, Map<Integer, List<CardDetails>> idMap, Map<Integer, String> altMap) {
        var attacks = original.attacks().stream()
                .map(CardDetailsProcessing::fixAttack)
                .toList();

        var cardIds = idMap.get(original.internalId())
                .stream()
                .map(CardDetails::cardId)
                .toList();

        var expansions = cardIds.stream()
                .map(EXPANSION_PATTERN::matcher)
                .filter(Matcher::matches)
                .map(matcher -> matcher.group(1))
                .toList();

        var alternateVersions = original.alternateVersions()
                .stream()
                .filter(altId -> altId != original.internalId())
                .map(altMap::get)
                .toList();

        return new UpdatedCardDetails(
                expansions,
                cardIds,
                original.hp(),
                original.energy(),
                original.name(),
                original.cardType(),
                original.evolutionType(),
                attacks,
                cleanAbility(original.ability()),
                mapRarity(original.rarity()),
                original.weakness(),
                original.retreat(),
                original.fullArt(),
                original.ex(),
                original.baby(),
                alternateVersions,
                original.artist()
        );
    }

    private static AbilityDetails cleanAbility(AbilityDetails ability) {
        if(ability == null || !ability.effect().contains("\n")) {
            return ability;
        }
        var effect = ability.effect().substring(0, ability.effect().indexOf('\n')).trim();
        return new AbilityDetails(ability.name(), effect);
    }

    private static AttackDetails fixAttack(AttackDetails original) {
        List<String> cost = original.cost();
        if (original.cost().size() == 1 && original.cost().get(0).equals("Unknown")) {
            cost = emptyList();
        }

        String name = original.name();
        String damage = original.damage();
        if (!original.damage().matches("\\d+[+x]?")) {
            name = original.name() + " " + original.damage();
            damage = "0";
        }
        return new AttackDetails(cost, name, damage, original.effect());
    }

    private static String mapRarity(String rarity) {
        return switch (rarity) {
            case "◊" -> "1d";
            case "◊◊" -> "2d";
            case "◊◊◊" -> "3d";
            case "◊◊◊◊" -> "4d";
            case "P" -> "p";
            case "✵" -> "1sh";
            case "✵✵" -> "2sh";
            case "☆" -> "1s";
            case "☆☆" -> "2s";
            case "☆☆☆" -> "3s";
            case "Crown Rare" -> "c";
            default -> throw new RuntimeException("unknown rarity: " + rarity);
        };
    }

    private static String compareCardId(String cardIdA, String cardIdB) {
        var expansionA = cardIdA.substring(0, cardIdA.lastIndexOf('-'));
        var expansionB = cardIdB.substring(0, cardIdB.lastIndexOf('-'));
        var cardNoA = Integer.parseInt(cardIdA.substring(cardIdA.lastIndexOf('-') + 1));
        var cardNoB = Integer.parseInt(cardIdB.substring(cardIdB.lastIndexOf('-') + 1));

        var expansionComparison = expansionA.compareTo(expansionB);

        if (expansionComparison < 0) {
            return cardIdA;
        } else if (expansionComparison > 0) {
            return cardIdB;
        }

        if (cardNoA - cardNoB <= 0) {
            return cardIdA;
        } else {
            return cardIdB;
        }
    }
}
