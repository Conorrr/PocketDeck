package io.restall.sharedex.classifier.collections;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restall.sharedex.classifier.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.nio.file.Files.newOutputStream;

public class CardIds {

    private static final ObjectMapper OM = new ObjectMapper();

    static {
        OM.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static void main(String[] args) throws IOException {
        var config = AppConfig.fromEnv();

        var allCards = OM.readValue(Files.newInputStream(config.rawCardDetailsPath()), new TypeReference<List<CardDetails>>() {
                }).stream()
                .sorted(CardIds::compareCardId)
                .toList();

        var orderedCardIds = allCards.stream()
                .filter(cardDetails -> !cardDetails.cardId().startsWith("P-"))
                .map(CardDetails::cardId)
                .toList();

        var promoCollection = allCards.stream()
                .filter(cardDetails -> cardDetails.cardId().startsWith("P-"))
                .toList();

        var orderedPromoCardIds = promoCollection.stream()
                .map(CardDetails::cardId)
                .toList();

        var numbered = IntStream.range(0, orderedCardIds.size())
                .boxed()
                .collect(Collectors.toMap(Function.identity(), orderedCardIds::get));

        var numberedPromo = IntStream.range(0, orderedPromoCardIds.size())
                .boxed()
                .collect(Collectors.toMap(i -> 8192 + i, orderedPromoCardIds::get));

        var allNumbered = Stream.of(numbered, numberedPromo)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        new ObjectMapper().writeValue(newOutputStream(config.cardListPath()), allNumbered);
    }

    private static int compareCardId(CardDetails cardA, CardDetails cardB) {
        var expansionA = cardA.cardId().substring(0, cardA.cardId().lastIndexOf('-'));
        var expansionB = cardB.cardId().substring(0, cardB.cardId().lastIndexOf('-'));
        var cardNoA = Integer.parseInt(cardA.cardId().substring(cardA.cardId().lastIndexOf('-') + 1));
        var cardNoB = Integer.parseInt(cardB.cardId().substring(cardB.cardId().lastIndexOf('-') + 1));

        var expansionComparison = expansionA.compareTo(expansionB);
        if(expansionComparison != 0) {
            return expansionComparison;
        }
        return cardNoA - cardNoB;
    }

}
