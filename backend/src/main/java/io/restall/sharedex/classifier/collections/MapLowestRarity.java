package io.restall.sharedex.classifier.collections;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restall.sharedex.classifier.AppConfig;
import lombok.SneakyThrows;
import org.apache.commons.math3.util.Pair;

import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.nio.file.Files.newOutputStream;

public class MapLowestRarity {

    private static final ObjectMapper OM = new ObjectMapper();

    static {
        OM.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @SneakyThrows
    public static void main(String[] args) {
        var config = AppConfig.fromEnv();
        var allCards = OM.readValue(Files.newInputStream(config.rawCardDetailsPath()), new TypeReference<List<CardDetails>>() {
        });

        var altMap = allCards.stream()
                .collect(Collectors.toMap(CardDetails::internalId, Function.identity(), MapLowestRarity::compareCardId));

        var cardMap = allCards.stream()
                .map(card -> Pair.create(
                        card.cardId(),
                        card.alternateVersions()
                                .stream()
                                .map(altMap::get)
                                .min(Comparator.comparing(MapLowestRarity::rarityToInt).thenComparing(MapLowestRarity::expansionToInt))
                                .map(CardDetails::cardId)
                ))
                .collect(Collectors.toMap(Pair::getKey, pair -> pair.getValue().orElseGet(pair::getKey)));

        OM.writeValue(newOutputStream(config.rarityMapPath()), cardMap);
    }

    private static CardDetails compareCardId(CardDetails cardA, CardDetails cardB) {
        var cardIdA = cardA.cardId();
        var cardIdB = cardB.cardId();
        var expansionA = cardIdA.substring(0, cardIdA.lastIndexOf('-'));
        var expansionB = cardIdB.substring(0, cardIdB.lastIndexOf('-'));
        var cardNoA = Integer.parseInt(cardIdA.substring(cardIdA.lastIndexOf('-') + 1));
        var cardNoB = Integer.parseInt(cardIdB.substring(cardIdB.lastIndexOf('-') + 1));

        var expansionComparison = expansionA.compareTo(expansionB);

        if (expansionComparison < 0) {
            return cardA;
        } else if (expansionComparison > 0) {
            return cardB;
        }

        if (cardNoA - cardNoB <= 0) {
            return cardA;
        } else {
            return cardB;
        }
    }

    private static int rarityToInt(CardDetails details) {
        return switch (details.rarity()) {
            case null -> 0;
            case "" -> 0;
            case "◊" -> 1;
            case "◊◊" -> 2;
            case "◊◊◊" -> 3;
            case "◊◊◊◊" -> 4;
            case "P" -> 5;
            case "✵" -> 6;
            case "✵✵" -> 7;
            case "✵✵✵" -> 8;
            case "☆" -> 9;
            case "☆☆" -> 10;
            case "☆☆☆" -> 11;
            case "Crown Rare" -> 12;
            default -> throw new RuntimeException("unknown rarity: " + details.rarity());
        };
    }

    private static int expansionToInt(CardDetails details) {
        return switch (details.expansion()) {
            case "A1" -> 0;
            case "A1a" -> 1;
            case "A2" -> 2;
            case "A2a" -> 3;
            case "A2b" -> 4;
            case "A3" -> 5;
            case "A3a" -> 6;
            case "A3b" -> 7;
            case "A4" -> 8;
            case "A4a" -> 9;
            case "A4b" -> 10;
            case "B1" -> 11;
            case "P-A" -> 12;
            case "P-B" -> 13;
            default -> throw new RuntimeException("unknown expansion: " + details.expansion());
        };
    }

}
