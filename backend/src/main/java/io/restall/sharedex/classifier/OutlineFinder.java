package io.restall.sharedex.classifier;

import lombok.Cleanup;
import lombok.experimental.UtilityClass;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;

@UtilityClass
public class OutlineFinder {

    static {
        nu.pattern.OpenCV.loadLocally();
    }

    public static List<Rect> findOutlines(Mat screenshot) {
        // Convert to grayscale
        @Cleanup var gray = new SafetyMat();
        Imgproc.cvtColor(screenshot, gray, Imgproc.COLOR_BGR2GRAY);

        // Apply Gaussian blur to reduce noise
        @Cleanup var blurred = new SafetyMat();
        Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);

        var clahe = Imgproc.createCLAHE(3.0, new Size(8, 8));
        clahe.apply(gray, gray);

//        // Replace adaptive threshold section with:
        @Cleanup var edges = new SafetyMat();
        Imgproc.Canny(blurred, edges, 30, 100);

        // Dilate to connect broken edges
        var kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(edges, edges, kernel, new Point(-1, -1), 2);

        List<MatOfPoint> contours = new ArrayList<>();
        @Cleanup var hierarchy = new SafetyMat();
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        List<Rect> cardRects = new ArrayList<>();

        // Approximate contour to polygon
        MatOfPoint2f approx = new MatOfPoint2f();

        for (var contour : contours) {
            var contour2f = new MatOfPoint2f(contour.toArray());
            Imgproc.approxPolyDP(contour2f, approx, 0.02 * Imgproc.arcLength(contour2f, true), true);

            // Check if polygon has 4 corners (rectangle)
            if (approx.rows() == 4) {
                Rect boundingBox = Imgproc.boundingRect(contour);
                double aspectRatio = (double) boundingBox.width / boundingBox.height;
                if (aspectRatio > 0.6 && aspectRatio < 0.8 && boundingBox.area() > 2000) {
                    cardRects.add(boundingBox);
                }
            }
            contour.release();
        }

        // filter out rectangles that aren't cards
        var regularRects = filterIrregularRects(cardRects);

        if (regularRects.isEmpty()) {
            return emptyList();
        }

        int avgWidth = findMedian(regularRects, (rect) -> rect.width);
        int avgHeight = findMedian(regularRects, (rect) -> rect.height);
        var gapSize = getGapSize(regularRects);

        int leftMostX = calcLeftmostX(regularRects, avgWidth, gapSize);
        int topMostY = calcTopmostY(regularRects, avgHeight, gapSize, (int) screenshot.size().height);

        var dx = avgWidth + gapSize;
        var dy = avgHeight + gapSize;

        int nRows = 5;
        int nCols = 10;

        List<Rect> allCards = new ArrayList<>();
        for (int row = 0; row < nRows; row++) {
            for (int col = 0; col < nCols; col++) {
                int x = leftMostX + col * dx;
                int y = topMostY + row * dy;
                if (screenshot.size().height >= y + avgHeight && screenshot.size().width >= x + avgWidth && x > 0 && y > 0) {
                    Rect r = new Rect(x, y, avgWidth, avgHeight);
                    allCards.add(r);
                }
            }
        }

        return allCards.stream()
                .map(cutout -> regularRects.stream()
                        .filter(r2 -> distanceBetweenRects(cutout, r2) < 10)
                        .findFirst()
                        .orElse(cutout)
                )
                .toList();
    }

    private static double distanceBetweenRects(Rect r1, Rect r2) {
        // Compute centers of each rectangle
        var c1 = new Point(r1.x + r1.width / 2.0, r1.y + r1.height / 2.0);
        var c2 = new Point(r2.x + r2.width / 2.0, r2.y + r2.height / 2.0);

        // Compute Euclidean distance between centers
        double dx = c1.x - c2.x;
        double dy = c1.y - c2.y;

        return Math.sqrt(dx * dx + dy * dy);
    }

    private static <T> int findMedian(List<T> list, ToIntFunction<T> extractor) {
        if (list.size() == 1) {
            return extractor.applyAsInt(list.getFirst());
        }
        return list.stream().mapToInt(extractor).sorted().limit(list.size() / 2).max().orElseThrow();
    }

    private static int calcLeftmostX(List<Rect> cardRects, int cardWidth, int gapSize) {
        var leftmostRectX = cardRects.stream().min(Comparator.comparing(rect -> rect.x)).get().x;
        var missingGaps = leftmostRectX / (cardWidth + gapSize);
        return leftmostRectX - (missingGaps * (cardWidth + gapSize));
    }

    private static int calcTopmostY(List<Rect> cardRects, int cardHeight, int gapSize, int imageHeight) {
        var bottommostRectY = cardRects.stream().max(Comparator.comparing(rect -> rect.y)).get().y;
        var missingGaps = (imageHeight - bottommostRectY) / (cardHeight + gapSize);
        var bottomMostY = bottommostRectY + (missingGaps * (cardHeight + gapSize));
        return bottomMostY - (5 * (cardHeight + gapSize));
    }

    private static int getGapSize(List<Rect> cardRects) {
        var maxGap = cardRects.stream().mapToInt(rect -> rect.width).average().orElse(0.0) / 6;
        return (int) cardRects.stream()
                .mapToInt(rect -> rect.x + rect.width)
                .flatMap(pos -> cardRects.stream().mapToInt(rect -> rect.x - pos))
                .filter(gap -> gap > 0 && gap < maxGap)
                .average()
                .orElse(0.0);
    }

    private static List<Rect> filterIrregularRects(List<Rect> rects) {
        var widthStats = new DescriptiveStatistics();
        rects.forEach(rect -> widthStats.addValue(rect.width));

        var mean = widthStats.getMean();
        var stdDev = widthStats.getStandardDeviation();

        var lowerBound = mean - stdDev;
        var upperBound = mean + stdDev;

        return rects.stream()
                .filter(rect -> rect.width > lowerBound && rect.width < upperBound)
                .toList();
    }

}
