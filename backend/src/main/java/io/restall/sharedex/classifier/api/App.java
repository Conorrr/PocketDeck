package io.restall.sharedex.classifier.api;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.UploadedFile;
import io.restall.sharedex.classifier.AppConfig;
import io.restall.sharedex.classifier.ColourPHashMatcher;
import io.restall.sharedex.classifier.OutlineFinder;
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
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    private final ImageDownloader imageDownloader = new ImageDownloader();
    private final PreviewGenerator previewGenerator;
    private final Path uploadDir;
    private final Path previewDir;
    private final String uiHost;
    private final Map<String, String> rarityMap;
    private final ExecutorService previewGeneratorExecutor = Executors.newFixedThreadPool(2);

    private final Map<String, LocalDateTime> lastReqTime = new ConcurrentHashMap<>();

    public App(AppConfig appConfig) throws IOException {
        if (!Files.exists(appConfig.uploadDir())) {
            Files.createDirectories(appConfig.uploadDir());
        }
        hashMatcher = new ColourPHashMatcher(appConfig.pHashBinary());
        cardRecogniser = new PokemonCardRecognizer(50, true);
        cardRecogniser.loadDatabase(appConfig.orbDatabaseBin());
        rarityMap = new ObjectMapper().readValue(Files.newInputStream(appConfig.rarityMapPath()), new TypeReference<>() {
        });
        compressor = new DeckCompressor(appConfig.cardListPath());
        previewGenerator = new PreviewGenerator(appConfig);
        uploadDir = appConfig.uploadDir();
        uiHost = appConfig.uiHost();
        previewDir = appConfig.previewDir();
    }

    public void start() {
        Javalin.create(config -> {
                    config.http.defaultContentType = "application/json";
                    config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.allowHost(uiHost)));
                })
                .get("/", ctx -> ctx.redirect(uiHost))
                .get("/preview/{deckId}.webp", this::handlePreview)
                .post("/upload", this::handleUpload)
                .post("/report", App::handleReport)
                .get("/deck/{deckId}", this::handleGetDeck)
                .start(7070);
    }

    public static void main(String[] args) throws IOException {
        new App(AppConfig.fromEnv()).start();
    }

    @SneakyThrows
    private void handlePreview(Context ctx) {
        var deckId = ctx.pathParam("deckId");
        var previewPath = previewDir.resolve(deckId + ".webp");

        if (!Files.exists(previewPath)) {
            var cardIds = compressor.decompress(deckId);
            // Reduce the risk of getting hit by some bot or someone malicious
            var future = previewGeneratorExecutor.submit(() -> previewGenerator.generatePreview(cardIds, deckId));
            future.get(2, TimeUnit.SECONDS);
            if (!future.isDone()) {
                future.cancel(false);
            }
        }
        if (Files.exists(previewPath)) {
            ctx.contentType(ContentType.IMAGE_WEBP)
                    .result(Files.newInputStream(previewPath));
        } else {
            ctx.status(HttpStatus.NOT_FOUND);
        }
    }

    @SneakyThrows
    private void handleUpload(Context ctx) {
        var ipAddress = getIpAddress(ctx);

        var capTime = LocalDateTime.now().minusSeconds(15);
        if (lastReqTime.getOrDefault(ipAddress, LocalDateTime.MIN).isAfter(capTime)) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(Map.of("error", "Rate Limit Reached. Please try again in 15 seconds."));
            return;
        }
        lastReqTime.put(ipAddress, LocalDateTime.now());

        UploadedFile file = ctx.uploadedFile("screenshot");
        var uri = ctx.formParam("uri");

        if (file == null && uri == null) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(Map.of("error", "No file uploaded"));
            return;
        }

        var requestId = UUID.randomUUID().toString();
        if (file != null) {
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
            processFile(ctx, requestId, file.content());

            storeImage(file.filename(), file.content(), requestId);
        } else {
            var imageIs = imageDownloader.downloadFile(uri);
            imageIs.mark((int) MAX_FILE_SIZE);
            if (imageIs == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(Map.of("error", "Unable to process file"));
                return;
            }
            processFile(ctx, requestId, imageIs);
            imageIs.reset();
            storeImage(uri, imageIs, requestId);
        }
    }

    private void processFile(Context ctx, String requestId, InputStream fileInputStream) {
        var screenshotMat = readImage(fileInputStream);

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
            var deckId = compressor.compress(cardIds);
            previewGeneratorExecutor.execute(() -> previewGenerator.generatePreview(cardIds, deckId));
            compressed = deckId;
        }

        ctx.status(HttpStatus.OK)
                .json(new UploadResult(compressed, requestId, results.size(), results));
    }

    private void storeImage(String filename, InputStream inputStream, String requestId) throws IOException {
        var fileExtension = getFileExtension(filename);
        Path target = uploadDir.resolve(requestId + fileExtension).normalize();

        if (!target.startsWith(uploadDir)) {
            log.warn("Attempt to write file to outside of dir");
            return;
        }
        Files.copy(inputStream, target);
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
        var dotIndex = filename.lastIndexOf('.');
        var qIndex = filename.indexOf('?') == -1 ? filename.length() : filename.indexOf('?');
        if (dotIndex == -1 || qIndex - dotIndex > 5) {
            return ".png";
        }
        return filename.substring(dotIndex, qIndex);
    }

    private static String getIpAddress(Context ctx) {
        var realIp = ctx.header("X-Real-IP");
        if (realIp != null) {
            return realIp;
        }
        return ctx.ip();
    }

}
