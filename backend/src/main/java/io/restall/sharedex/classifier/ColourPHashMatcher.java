package io.restall.sharedex.classifier;

import lombok.SneakyThrows;
import org.apache.commons.math3.util.Pair;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ColourPHashMatcher {

    static {
        nu.pattern.OpenCV.loadLocally();
    }

    private final Map<String, long[]> hashes;

    public ColourPHashMatcher() {
        hashes = new HashMap<>();
    }

    @SneakyThrows
    public ColourPHashMatcher(Path hashBinaryPath) {
        hashes = loadHashesBinary(hashBinaryPath);
    }

    public static long computePHash(Mat image) {
        // Convert to float for DCT
        Mat floatImg = new Mat();
        image.convertTo(image, CvType.CV_32F);

        // Apply DCT (Discrete Cosine Transform)
        Mat dct = new Mat();
        Core.dct(image, image);

        // Extract top-left 8x8 (low frequency components)
        Mat dctLowFreq = image.submat(0, 8, 0, 8);

        // Calculate median
        double[] dctArray = new double[64];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                dctArray[i * 8 + j] = dctLowFreq.get(i, j)[0];
            }
        }
        double median = calculateMedian(dctArray);

        // Build hash: 1 if above median, 0 if below
        long hash = 0L;
        for (int i = 0; i < 64; i++) {
            if (dctArray[i] > median) {
                hash |= (1L << i);
            }
        }

        // Cleanup
        floatImg.release();
        dct.release();
        dctLowFreq.release();

        return hash;
    }

    private static Mat prepareImage(Mat original) {
        Mat prepared = new Mat();

        Imgproc.resize(original, prepared, new Size(32, 32), 0, 0, Imgproc.INTER_AREA);
        Imgproc.cvtColor(prepared, prepared, Imgproc.COLOR_BGR2Lab);

        return prepared;
    }

    private static int hammingDistance(long hash1, long hash2) {
        return Long.bitCount(hash1 ^ hash2);
    }

    private static double similarity(long[] hash1, long[] hash2) {
        int totalDistance = 0;
        int totalBits = 64 * hash1.length; // each long has 64 bits

        for (int i = 0; i < hash1.length; i++) {
            totalDistance += hammingDistance(hash1[i], hash2[i]);
        }

        return (totalBits - totalDistance) * 100.0 / totalBits;
    }

    private static double calculateMedian(double[] array) {
        double[] sorted = array.clone();
        Arrays.sort(sorted);
        int len = sorted.length;
        if (len % 2 == 0) {
            return (sorted[len / 2 - 1] + sorted[len / 2]) / 2.0;
        } else {
            return sorted[len / 2];
        }
    }

    private static long[] computeColourPHash(Mat src) {
        var prepared = prepareImage(src);

        long[] hashes = new long[3]; // L, a, b channels
        for (int c = 0; c < 3; c++) {
            Mat channel = new Mat();
            Core.extractChannel(prepared, channel, c);

            hashes[c] = computePHash(channel);
            channel.release();
        }
        prepared.release();
        return hashes; // 192-bit hash as three longs
    }

    public List<Pair<String, Double>> findTopMatches(Mat cardImage, int limit, double threshold) {
        var cardHash = computeColourPHash(cardImage);

        List<Pair<String, Double>> topMatches = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : hashes.entrySet()) {
            double sim = similarity(cardHash, entry.getValue());
            if (sim >= threshold) {
                topMatches.add(Pair.create(entry.getKey(), sim));
            }
        }

        topMatches.sort((p1, p2) -> Double.compare(p2.getSecond(), p1.getSecond()));

        if (topMatches.size() > limit) {
            topMatches = topMatches.subList(0, limit);
        }

        return topMatches;
    }

    public void addImage(String name, Mat mat) {
        hashes.put(name, computeColourPHash(mat));
    }

    private void saveHashesBinary(Path filepath) throws IOException {
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(filepath))) {
            out.writeInt(hashes.size());
            for (Map.Entry<String, long[]> entry : hashes.entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeLong(entry.getValue()[0]);
                out.writeLong(entry.getValue()[1]);
                out.writeLong(entry.getValue()[2]);
            }
        }
    }

    @SneakyThrows
    private static Map<String, long[]> loadHashesBinary(Path filepath) {
        Map<String, long[]> hashes = new HashMap<>();
        try (DataInputStream in = new DataInputStream(Files.newInputStream(filepath))) {
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                String name = in.readUTF();
                long hash1 = in.readLong();
                long hash2 = in.readLong();
                long hash3 = in.readLong();
                hashes.put(name, new long[]{hash1, hash2, hash3});
            }
        }
        return hashes;
    }

    public static void main(String[] args) throws IOException {
        var start = System.currentTimeMillis();
        var config = AppConfig.fromEnv();

        var matcher = new ColourPHashMatcher();
        Files.list(config.cardImageDir())
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                            name.endsWith(".png") || name.endsWith(".bmp") ||
                            name.endsWith(".webp");
                })
                .forEach(path -> matcher.addImage(
                                path.getFileName().toString().replaceFirst("[.][^.]+$", ""),
                                Imgcodecs.imread(path.toAbsolutePath().toString())
                        )
                );

        matcher.saveHashesBinary(config.pHashBinary());
        System.out.printf("Generated and Saved Colour PHash DB. Took: %sms%n", System.currentTimeMillis() - start);
    }

}
