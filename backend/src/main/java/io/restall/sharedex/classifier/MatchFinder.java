package io.restall.sharedex.classifier;

import io.restall.sharedex.classifier.api.SafeImageDecoder;
import io.restall.sharedex.classifier.opencv.PokemonCardRecognizer;
import io.restall.sharedex.classifier.opencv.Prediction;
import lombok.Cleanup;
import lombok.SneakyThrows;
import org.apache.commons.math3.util.Pair;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.restall.sharedex.classifier.opencv.PokemonCardRecognizer.isBlank;

public class MatchFinder {

    private final ColourPHashMatcher hashMatcher;
    private final PokemonCardRecognizer cardRecogniser;
    private final SafeImageDecoder imageDecoder = new SafeImageDecoder();
    private final Map<String, String> rarityMap;


    public MatchFinder(ColourPHashMatcher hashMatcher, PokemonCardRecognizer cardRecogniser,
                       Map<String, String> rarityMap) {
        this.hashMatcher = hashMatcher;
        this.cardRecogniser = cardRecogniser;
        this.rarityMap = rarityMap;
    }

    public List<Prediction> findMatches(InputStream inputStream) {
        var screenshotMat = readImage(inputStream);

        var outlines = OutlineFinder.findOutlines(screenshotMat);
        return outlines.stream()
                .flatMap(rect -> {
                    @Cleanup var cutout = new SafetyMat(screenshotMat, rect);
                    if (isBlank(cutout)) {
                        return Optional.<Prediction>empty().stream();
                    }
                    var roughMatches = hashMatcher.findTopMatches(cutout, 5, 70.0);
                    if (roughMatches.isEmpty()) {
                        return Optional.<Prediction>empty().stream();
                    }
                    if (roughMatches.size() > 1) {
                        // if the multiple close matches use ORB to refine search
                        if (roughMatches.get(0).getValue() - roughMatches.get(1).getValue() < roughMatches.get(0).getValue() * 0.02) {
                            var roughMatchCardIds = roughMatches.stream().map(Pair::getKey).collect(Collectors.toSet());
                            return cardRecogniser.recognize(cutout, 1, roughMatchCardIds).getBestMatch()
                                    .map(prediction -> {
                                        var hashScore = roughMatches.stream().filter(pair -> pair.getKey().equals(prediction.cardName()))
                                                .map(Pair::getValue)
                                                .findFirst()
                                                .orElse(0.0);
                                        return new Prediction(prediction.cardName(), hashScore, prediction.matchCount(), prediction.confidence());
                                    })
                                    .stream();
                        }
                    }
                    return Stream.of(new Prediction(roughMatches.get(0).getKey(), roughMatches.get(0).getValue(), 0, 0));
                })
                .map(prediction -> new Prediction(rarityMap.getOrDefault(prediction.cardName(), prediction.cardName()), prediction.hashScore(), prediction.matchCount(), prediction.confidence()))
                .toList();
    }

    @SneakyThrows
    private Mat readImage(InputStream is) {
        return imageDecoder.safeDecode(
                is.readAllBytes(),
                5_000_000,        // max encoded bytes (5 MB)
                8000,             // max width
                8000,             // max height
                50_000_000L,      // max pixels
                3000,             // 3 second decode timeout
                Imgcodecs.IMREAD_COLOR
        );
    }


}
