package io.restall.sharedex.classifier.api;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.UploadedFile;
import io.restall.sharedex.classifier.AppConfig;
import io.restall.sharedex.classifier.ColourPHashMatcher;
import io.restall.sharedex.classifier.MatchFinder;
import io.restall.sharedex.classifier.bot.Bot;
import io.restall.sharedex.classifier.bot.DeckRepository;
import io.restall.sharedex.classifier.bot.RedditClient;
import io.restall.sharedex.classifier.opencv.PokemonCardRecognizer;
import io.restall.sharedex.classifier.opencv.Prediction;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Slf4j
public class App {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private final DeckCompressor compressor;
    private final ImageDownloader imageDownloader = new ImageDownloader();
    private final PreviewGenerator previewGenerator;
    private final MatchFinder matchFinder;
    private final DeckRepository deckRepo;
    private final Path uploadDir;
    private final Path previewDir;
    private final String uiHost;
    private final ExecutorService previewGeneratorExecutor = Executors.newFixedThreadPool(2);
    private final Bot bot;

    private final Map<String, LocalDateTime> lastReqTime = new ConcurrentHashMap<>();

    public App(AppConfig appConfig) throws IOException {
        if (!Files.exists(appConfig.uploadDir())) {
            Files.createDirectories(appConfig.uploadDir());
        }

        var om = new ObjectMapper();
        om.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.registerModule(new JavaTimeModule());

        var hashMatcher = new ColourPHashMatcher(appConfig.pHashBinary());
        var cardRecogniser = new PokemonCardRecognizer(50, true);
        cardRecogniser.loadDatabase(appConfig.orbDatabaseBin());
        var rarityMap = om.readValue(Files.newInputStream(appConfig.rarityMapPath()), new TypeReference<Map<String, String>>() {
        });

        matchFinder = new MatchFinder(hashMatcher, cardRecogniser, rarityMap);
        compressor = new DeckCompressor(appConfig.cardListPath());
        previewGenerator = new PreviewGenerator(appConfig);
        uploadDir = appConfig.uploadDir();
        uiHost = appConfig.uiHost();
        previewDir = appConfig.previewDir();

        var redditClient = new RedditClient(om);
        deckRepo = new DeckRepository(appConfig.dbUrl(), appConfig.dbUser(), appConfig.dbPassword());
        bot = new Bot(redditClient, deckRepo, imageDownloader, matchFinder, compressor);
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
                .get("/latest", this::handleLatest)
                .start(7070);

        bot.start();
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

    private void processFile(Context ctx, String requestId, InputStream inputStream) {
        var results = matchFinder.findMatches(inputStream);

        var cardIds = results.stream().map(Prediction::cardName).toList();
        String compressed = null;
        if (results.size() == 20) {
            var deckId = compressor.compress(cardIds);
            previewGeneratorExecutor.execute(() -> previewGenerator.generatePreview(cardIds, deckId));
            compressed = deckId;
            deckRepo.insertDeck(deckId);
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

    private static void handleReport(Context ctx) {
        // map from json object
        // { uploadId: uuid }
    }

    private void handleLatest(Context ctx) {
        var latestDecks = deckRepo.getLatestDecks();
        ctx.status(HttpStatus.OK)
                .json(latestDecks);
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
