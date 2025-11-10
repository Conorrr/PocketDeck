package io.restall.sharedex.classifier.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restall.sharedex.classifier.AppConfig;
import io.restall.sharedex.classifier.collections.CardDetails;
import lombok.SneakyThrows;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.Files.newInputStream;

public class DeckCompressor {

    private final Map<Integer, String> idMap;
    private final Map<String, Integer> cardMap;

    @SneakyThrows
    public DeckCompressor(Path idMapFilePath) {
        var om = new ObjectMapper();
        this.idMap = om.readValue(newInputStream(idMapFilePath), new TypeReference<HashMap<Integer,String>>(){});;
        this.cardMap = idMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    public String compress(List<String> cards) {
        var cardIds = cards.stream()
                .map(cardMap::get)
                .toList();
        return encode(collapse(cardIds));
    }

    public List<String> decompress(String compressedDeck) {
        return expand(decode(compressedDeck)).stream()
                .map(idMap::get)
                .toList();
    }

    private static List<Integer> collapse(List<Integer> numbers) {
        List<Integer> result = new ArrayList<>();
        if (numbers.isEmpty()) {
            return result;
        }

        int prev = numbers.getFirst();
        boolean repeated = false;

        for (int i = 1; i < numbers.size(); i++) {
            int curr = numbers.get(i);
            if (curr == prev) {
                repeated = true;
            } else {
                result.add(repeated ? -prev : prev);
                prev = curr;
                repeated = false;
            }
        }

        // handle the last group
        result.add(repeated ? -prev : prev);
        return result;
    }

    public static List<Integer> expand(List<Integer> numbers) {
        List<Integer> result = new ArrayList<>();

        for (int n : numbers) {
            if (n < 0) {
                int value = -n;
                result.add(value);
                result.add(value); // repeat twice
            } else {
                result.add(n);
            }
        }

        return result;
    }

    /**
     * Encodes a list of up to 20 card entries into a URL-safe base64 string.
     * Each integer represents a card where:
     * - Positive values (1-32767): card ID with count of 1
     * - Negative values (-1 to -32767): card ID (absolute value) with count of 2
     *
     * Uses 2 bytes per card: 15 bits for ID, 1 bit for count
     */
    private String encode(List<Integer> cards) {
        if (cards.size() > 20) {
            throw new IllegalArgumentException("List cannot contain more than 20 cards");
        }

        byte[] bytes = new byte[cards.size() * 2];
        int byteIndex = 0;

        for (int card : cards) {
            int cardId;
            boolean isDouble;

            if (card < 0) {
                cardId = -card;
                isDouble = true;
            } else {
                cardId = card;
                isDouble = false;
            }

            if (cardId <= 0 || cardId > 32767) {
                throw new IllegalArgumentException("Card ID must be between 1 and 32767, got: " + cardId);
            }

            // Encode: 15 bits for card ID, 1 bit for count (MSB)
            int encoded = cardId | (isDouble ? 0x8000 : 0);

            // Write as big-endian
            bytes[byteIndex++] = (byte) (encoded >> 8);
            bytes[byteIndex++] = (byte) encoded;
        }

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Decodes a URL-safe base64 string back into a list of card entries.
     * Returns integers where:
     * - Positive values: card ID with count of 1
     * - Negative values: card ID with count of 2
     */
    private List<Integer> decode(String encoded) {
        byte[] bytes = Base64.getUrlDecoder().decode(encoded);

        if (bytes.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid encoded string length");
        }

        List<Integer> cards = new ArrayList<>();

        for (int i = 0; i < bytes.length; i += 2) {
            int high = bytes[i] & 0xFF;
            int low = bytes[i + 1] & 0xFF;
            int encoded_val = (high << 8) | low;

            boolean isDouble = (encoded_val & 0x8000) != 0;
            int cardId = encoded_val & 0x7FFF;

            cards.add(isDouble ? -cardId : cardId);
        }

        return cards;
    }

    public static void main(String[] args) {
        var config = AppConfig.fromEnv();
        var compressor = new DeckCompressor(config.cardListPath());
        var cardIds = List.of("A3a-21", "A3a-21", "A4-71", "A3-165", "A4-171", "A4-171", "A2a-95", "A2a-96", "P-A-5", "P-A-5", "P-A-6", "A4-151", "A4-151", "A2-147", "P-A-7", "P-A-7", "A2-150", "A2b-71", "A2b-71", "A3-208");
        var compressed = compressor.compress(cardIds);
        System.out.println(compressed);
        var decompressed = compressor.decompress(compressed);
        System.out.println(cardIds.equals(decompressed));
    }

}
