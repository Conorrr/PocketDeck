package io.restall.sharedex.classifier.api;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.UploadedFile;
import io.restall.sharedex.classifier.AppConfig;
import io.restall.sharedex.classifier.OutlineFinder;
import io.restall.sharedex.classifier.ColourPHashMatcher;
import io.restall.sharedex.classifier.SafetyMat;
import io.restall.sharedex.classifier.opencv.PokemonCardRecognizer;
import io.restall.sharedex.classifier.opencv.Prediction;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.restall.sharedex.classifier.opencv.PokemonCardRecognizer.isBlank;

@Slf4j
public class App {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private final DeckCompressor compressor;
    private final ColourPHashMatcher hashMatcher;
    private final PokemonCardRecognizer cardRecogniser;
    private final SafeImageDecoder imageDecoder = new SafeImageDecoder();
    private final Path uploadDir;
    private final Map<String, String> rarityMap;

    public App(AppConfig config) throws IOException {
        if (!Files.exists(config.uploadDir())) {
            Files.createDirectories(config.uploadDir());
        }
        hashMatcher = new ColourPHashMatcher(config.pHashBinary());
        cardRecogniser = new PokemonCardRecognizer(50, true);
        cardRecogniser.loadDatabase(config.orbDatabaseBin());
        rarityMap = new ObjectMapper().readValue(Files.newInputStream(config.rarityMapPath()), new TypeReference<>() {
        });
        compressor = new DeckCompressor(config.cardListPath());
        uploadDir = config.uploadDir();
    }

    public void start() {
        Javalin.create(config -> {
                    config.http.defaultContentType = "application/json";
                    config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
                })
                .get("/", ctx -> ctx.contentType(ContentType.HTML)
                        .result("""
                                <form method="post" action="/upload" enctype="multipart/form-data">
                                    <input type="file" name="screenshot">
                                    <button>Submit</button>
                                </form>
                                """))
                .post("/upload", this::handleUpload)
                .post("/report", App::handleReport)
                .get("/deck/{deckId}", this::handleGetDeck)
                .start(7070);
    }

    public static void main(String[] args) throws IOException {
        new App(AppConfig.fromEnv()).start();
    }

    @SneakyThrows
    private void handleUpload(Context ctx) {
        UploadedFile file = ctx.uploadedFile("screenshot");
        if (file == null) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(Map.of("error", "No file uploaded"));
            return;
        }

        if (!file.contentType().startsWith("image/")) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(Map.of("error", "Only image files are allowed"));
            return;
        }

        if (file.size() > MAX_FILE_SIZE) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(Map.of("error", "File too large (max 5MB)"));
            return;
        }

        var requestId = UUID.randomUUID().toString();
        var screenshotMat = readImage(file.content());

        var outlines = OutlineFinder.findOutlines(screenshotMat);
        var results = outlines.stream()
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

        var cardIds = results.stream().map(Prediction::cardName).toList();
        String compressed = null;
        if (results.size() == 20) {
            compressed = compressor.compress(cardIds);
        }

        ctx.status(HttpStatus.OK)
                .json(new UploadResult(compressed, requestId, results.size(), results));

        storeImage(file, requestId);
    }

    private void storeImage(UploadedFile file, String requestId) throws IOException {
        var fileExtension = getFileExtension(file.filename());
        Path target = uploadDir.resolve(requestId + fileExtension).normalize();

        if (!target.startsWith(uploadDir)) {
            log.warn("Attempt to write file to outside of dir");
            return;
        }
        Files.copy(file.content(), target);
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

    private static void handleReport(Context ctx) {
        // map from json object
        // { uploadId: uuid }
    }

    private void handleGetDeck(Context ctx) {
        var deckId = ctx.pathParam("deckId");

        var cards = compressor.decompress(deckId);
        if (cards == null || cards.size() != 20) {
            ctx.status(HttpStatus.NOT_FOUND);
        } else {
            ctx.status(HttpStatus.OK)
                    .json(Map.of("cards", cards));
        }
    }

    private static String getFileExtension(String filename) {
        var dotIndex = filename.indexOf('.');
        if (dotIndex == -1) {
            return "";
        }
        return filename.substring(dotIndex);
    }

}
