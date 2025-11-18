package io.restall.sharedex.classifier.bot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restall.sharedex.classifier.AppConfig;
import io.restall.sharedex.classifier.ColourPHashMatcher;
import io.restall.sharedex.classifier.MatchFinder;
import io.restall.sharedex.classifier.api.DeckCompressor;
import io.restall.sharedex.classifier.api.ImageDownloader;
import io.restall.sharedex.classifier.opencv.PokemonCardRecognizer;
import io.restall.sharedex.classifier.opencv.Prediction;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.util.Collections.emptyList;

@Slf4j
public class Bot {

    private final RedditClient redditClient;
    private final DeckRepository postRepo;
    private final ImageDownloader imageDownloader;
    private final MatchFinder matchFinder;
    private final DeckCompressor deckCompressor;

    private Deque<String> latestIds;
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private static final int LOOP_PERIOD_MINUTES = 5;

    public Bot(RedditClient redditClient, DeckRepository postRepo, ImageDownloader imageDownloader,
               MatchFinder matchFinder, DeckCompressor deckCompressor) {
        this.redditClient = redditClient;
        this.postRepo = postRepo;
        this.imageDownloader = imageDownloader;
        this.matchFinder = matchFinder;
        this.deckCompressor = deckCompressor;

        latestIds = new ConcurrentLinkedDeque<>(postRepo.getLatestPostsIds().reversed());
    }

    public void start() {
        executorService.scheduleAtFixedRate(this::run, 0, LOOP_PERIOD_MINUTES, TimeUnit.MINUTES);
    }

    public void stop() {
        executorService.shutdown();
    }

    private void run() {
        try {
            var latest = redditClient.getLatest("ptcgp");

            var unprocessed = latest.stream()
                    .filter(post -> !latestIds.contains(post.id()))
                    .collect(Collectors.toSet());
            log.info("Fetched {} newest posts, {} are new", latest.size(), unprocessed.size());

            unprocessed.forEach(this::processPost);

            unprocessed.forEach(post -> {
                latestIds.pollFirst();
                latestIds.addLast(post.id());
            });
        } catch (Exception e) {
            log.error("Error Running Reddit Bot", e);
        }
    }

    private void processPost(RedditPost post) {
        try {
            var images = getImageUrl(post);

            var deckIds = images.stream()
                    .flatMap(url -> processImage(url).stream())
                    .toList();

            postRepo.insertPost(post, images);
            postRepo.insertDecks(deckIds, post.id());
        } catch (Exception e) {
            log.error("Error processing post: {}. Continuing...", post.id(), e);
        }
    }

    private Optional<String> processImage(String url) {
        try (var imageStream = imageDownloader.downloadFile(url)) {
            var matches = matchFinder.findMatches(imageStream);
            if (matches.size() == 20) {
                log.info("Found deck");
                var cards = matches.stream().map(Prediction::cardName).toList();
                return Optional.of(deckCompressor.compress(cards));
            }
        } catch (Exception e) {
            log.error("Error processing image: {}. Continuing...", url, e);
        }
        return Optional.empty();
    }

    public static void main(String[] args) throws IOException {
        var appConfig = AppConfig.fromEnv();

        var om = new ObjectMapper();
        om.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.registerModule(new JavaTimeModule());

        var redditClient = new RedditClient(om);
        var postRepo = new DeckRepository(
                "jdbc:postgresql://10.20.30.104:5432/pocketdeck", "pocketdeck", "EGkF7cGSPqlkBto8"
        );

        var imageDownloader = new ImageDownloader();
        var hashMatcher = new ColourPHashMatcher(appConfig.pHashBinary());
        var cardRecogniser = new PokemonCardRecognizer(50, true);
        cardRecogniser.loadDatabase(appConfig.orbDatabaseBin());
        var rarityMap = new ObjectMapper().readValue(Files.newInputStream(appConfig.rarityMapPath()), new TypeReference<Map<String, String>>() {
        });

        var matchFinder = new MatchFinder(hashMatcher, cardRecogniser, rarityMap);
        var deckCompressor = new DeckCompressor(appConfig.cardListPath());

        var bot = new Bot(redditClient, postRepo, imageDownloader, matchFinder, deckCompressor);

        bot.run();
    }

    private static List<List<String>> getImageUrls(List<RedditPost> posts) {
        return posts.stream()
                .map(Bot::getImageUrl)
                .toList();
    }

    private static List<String> getImageUrl(RedditPost post) {
        if (post.mediaMetadata() != null) {
            return post.mediaMetadata()
                    .values()
                    .stream()
                    .map(metadata -> metadata.sourceData().u().replaceAll("&amp;", "&"))
                    .toList();
        } else if (post.url() != null && post.url().startsWith("https://i.redd.it/")) {
            return Collections.singletonList(post.url());
        }
        return emptyList();
    }
}
