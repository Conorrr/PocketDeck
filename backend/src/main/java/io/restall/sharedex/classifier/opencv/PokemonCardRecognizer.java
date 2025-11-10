package io.restall.sharedex.classifier.opencv;

import io.restall.sharedex.classifier.AppConfig;
import org.opencv.core.*;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;

public class PokemonCardRecognizer {

    static {
        nu.pattern.OpenCV.loadLocally();
    }

    private Map<String, CardFeatures> cardDatabase;
    private final ORB featureDetector;
    private final DescriptorMatcher matcher;

    private static final float MATCH_RATIO_THRESHOLD = 0.75f;
    private static final int MIN_MATCH_COUNT = 10;

    public PokemonCardRecognizer(int maxFeatures, boolean fast) {
        if (fast) {
            featureDetector = ORB.create(
                    maxFeatures,
                    1.5f,
                    2,
                    31,
                    0,
                    2,
                    ORB.FAST_SCORE,
                    31,
                    40
            );
        } else {
            featureDetector = ORB.create(
                    maxFeatures,
                    1.2f,
                    8,
                    31,
                    0,
                    2,
                    ORB.HARRIS_SCORE,
                    31,
                    20
            );
        }

        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        cardDatabase = new HashMap<>();
    }

    private Mat preprocessImage(Mat image) {
        Mat processed = new Mat();

        // Convert to grayscale for feature detection
        Imgproc.cvtColor(image, processed, COLOR_BGR2GRAY);

        // Slight Gaussian blur to reduce noise
        Imgproc.GaussianBlur(processed, processed, new Size(3, 3), 0);

        return processed;
    }

    public void loadReferenceCards(Path referenceDir) throws IOException {
        if (!Files.exists(referenceDir)) {
            throw new IOException("Reference directory not found: " + referenceDir);
        }

        System.out.println("Loading reference cards...");

        List<Path> imageFiles = Files.list(referenceDir)
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                            name.endsWith(".png") || name.endsWith(".bmp") ||
                            name.endsWith(".webp");
                })
                .toList();

        int loaded = 0;
        for (Path imagePath : imageFiles) {
            String cardName = imagePath.getFileName().toString().replaceFirst("[.][^.]+$", "");

            Mat image = Imgcodecs.imread(imagePath.toString());
            if (image.empty()) {
                System.err.println("Failed to load: " + imagePath);
                continue;
            }

            cardDatabase.put(cardName, extractFeatures(image, cardName));

            image.release();
            loaded++;
        }

        System.out.println("✓ Loaded " + loaded + " reference cards");
    }

    private CardFeatures extractFeatures(Mat image, String cardName) {
        Mat processed = preprocessImage(image);
        MatOfKeyPoint keypoints = new MatOfKeyPoint();
        Mat descriptors = new Mat();

        featureDetector.detectAndCompute(processed, new Mat(), keypoints, descriptors);
        KeyPoint[] kpArray = keypoints.toArray();
        for (KeyPoint kp : kpArray) {
            kp.angle = 0;
        }
        keypoints.fromArray(kpArray);

        processed.release();

        return new CardFeatures(cardName, keypoints, descriptors);
    }

    public static boolean isBlank(Mat image) {
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, COLOR_BGR2GRAY);

        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(gray, mean, stddev);

        double std = stddev.get(0, 0)[0];
        gray.release();

        return std < 25.0;
    }

    public PredictionResult recognize(String queryImagePath) {
        Mat queryImage = Imgcodecs.imread(queryImagePath);
        if (queryImage.empty()) {
            throw new RuntimeException("Failed to load image: " + queryImagePath);
        }
        return recognize(queryImage, 7, null);
    }

    public PredictionResult recognize(Mat queryImage, int topK, Set<String> limit) {
        if (isBlank(queryImage)) {
            return new PredictionResult(emptyList());
        }

        Mat processed = preprocessImage(queryImage);
        MatOfKeyPoint queryKeypoints = new MatOfKeyPoint();
        Mat queryDescriptors = new Mat();

        featureDetector.detectAndCompute(processed, new Mat(), queryKeypoints, queryDescriptors);

        var topMatches = cardDatabase.entrySet().stream()
                .filter(entry -> limit == null || limit.contains(entry.getKey()))
                .map(entry -> {
                    CardFeatures refCard = entry.getValue();

                    // Find matches using KNN
                    List<MatOfDMatch> knnMatches = new ArrayList<>();
                    matcher.knnMatch(queryDescriptors, refCard.descriptors(), knnMatches, 2);

                    // Apply Lowe's ratio test
                    List<DMatch> goodMatches = knnMatches.stream()
                            .flatMap(match -> Arrays.stream(match.toArray()))
                            .collect(Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    list -> {
                                        List<DMatch> filtered = new ArrayList<>();
                                        for (int i = 0; i + 1 < list.size(); i += 2) {
                                            DMatch m1 = list.get(i);
                                            DMatch m2 = list.get(i + 1);
                                            if (m1.distance < MATCH_RATIO_THRESHOLD * m2.distance) {
                                                filtered.add(m1);
                                            }
                                        }
                                        return filtered;
                                    }
                            ));

                    double score = calculateMatchScore(goodMatches, queryDescriptors.rows(), refCard.descriptors().rows());

                    return new CardMatch(entry.getKey(), goodMatches.size(), score);
                })
                .sorted(Comparator.comparing((CardMatch match) -> match.score).reversed())
                .limit(topK)
                .map(m -> new Prediction(m.cardName, 0.0, m.matchCount, m.score))
                .toList();

        // Cleanup
        processed.release();
        queryImage.release();
        queryDescriptors.release();

        return new PredictionResult(topMatches);
    }

    private double calculateMatchScore(List<DMatch> matches, int queryDescCount, int refDescCount) {
        if (matches.isEmpty()) {
            return 0.0;
        }

        double avgDistance = matches.stream()
                .mapToDouble(m -> m.distance)
                .average()
                .orElse(Double.MAX_VALUE);

        int matchCount = matches.size();
        double matchRatio = (double) matchCount / Math.min(queryDescCount, refDescCount);

        double distanceScore = Math.max(0, 1.0 - (avgDistance / 100.0));

        return (matchCount * 0.4) + (matchRatio * 100 * 0.4) + (distanceScore * 100 * 0.2);
    }

    public void saveDatabase(Path filepath) {
        CardFeatureMarshaller.write(cardDatabase, filepath);
    }

    public void loadDatabase(Path filepath) {
        cardDatabase = CardFeatureMarshaller.read(filepath);
    }

    private static class CardMatch {
        String cardName;
        int matchCount;
        double score;

        CardMatch(String cardName, int matchCount, double score) {
            this.cardName = cardName;
            this.matchCount = matchCount;
            this.score = score;
        }
    }

    public static void main(String[] args) throws Exception {
        var start = System.currentTimeMillis();
        var config = AppConfig.fromEnv();

        PokemonCardRecognizer recognizer = new PokemonCardRecognizer(50, false);

        recognizer.loadReferenceCards(config.cardImageDir());
        recognizer.saveDatabase(config.orbDatabaseBin());

        System.out.printf("Generated and Saved Colour PHash DB. Took: %sms%n", System.currentTimeMillis() - start);
    }
}