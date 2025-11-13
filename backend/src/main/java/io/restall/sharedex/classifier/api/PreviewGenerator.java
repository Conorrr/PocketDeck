package io.restall.sharedex.classifier.api;

import io.restall.sharedex.classifier.AppConfig;
import lombok.SneakyThrows;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PreviewGenerator {

    static {
        nu.pattern.OpenCV.loadLocally();
    }

    private final Map<String, Mat> cardImages;

    private final Mat colPad;
    private final Mat rowPad;
    private final Scalar bgColour;
    private final Path previewDir;

    private final MatOfInt compression = new MatOfInt(
            Imgcodecs.IMWRITE_WEBP_QUALITY, 30
    );

    @SneakyThrows
    public PreviewGenerator(AppConfig config) {
        bgColour = new Scalar(113, 116, 120);
        previewDir = config.previewDir();

        cardImages = Files.list(config.cardImageDir())
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                            name.endsWith(".png") || name.endsWith(".bmp") ||
                            name.endsWith(".webp");
                })
                .collect(Collectors.toMap(
                        PreviewGenerator::cardIdFromPath,
                        path -> replaceTransparent(Imgcodecs.imread(path.toString(), Imgcodecs.IMREAD_UNCHANGED), bgColour)
                ));

        int paddingSize = 20;
        int height = cardImages.get("A1-1").rows();
        int type = cardImages.get("A1-1").type();
        int width = cardImages.get("A1-1").cols();

        colPad = new Mat(height, paddingSize, type, bgColour);
        rowPad = new Mat(paddingSize, width * 5 + 6 * paddingSize, type, bgColour);
    }

    private static String cardIdFromPath(Path cardFilePath) {
        var filename = cardFilePath.getFileName().toString();
        return filename.substring(0, filename.indexOf('.'));
    }

    public void generatePreview(List<String> cardIds, String deckId) {
        if (cardIds.size() != 20) {
            return;
        }
        var rows = new ArrayList<Mat>(9);
        rows.add(rowPad);
        for (int i = 0; i < 4; i++) {
            rows.add(drawRow(cardIds.subList(i * 5, Math.min((i + 1) * 5, 20))));
            rows.add(rowPad);
        }

        var allCards = new Mat();
        Core.vconcat(rows, allCards);

        Imgproc.resize(allCards, allCards, new Size(allCards.width() / 2, allCards.height() / 2));

        Imgcodecs.imwrite(previewDir.resolve(deckId + ".webp").toString(), allCards, compression);
    }

    private Mat drawRow(List<String> ids) {
        var rowImages = new ArrayList<Mat>(11);
        rowImages.add(colPad);
        for (int i = 0; i < 5; i++) {
            rowImages.add(cardImages.get(ids.get(i)));
            rowImages.add(colPad);
        }

        var row = new Mat();
        Core.hconcat(rowImages, row);
        return row;
    }

    private static Mat replaceTransparent(Mat src, Scalar bgColorBGR) {
        // If there's no alpha channel, just return a copy
        if (src.channels() < 4) {
            return src.clone();
        }

        // Split channels: [B, G, R, A]
        List<Mat> chans = new ArrayList<>();
        Core.split(src, chans);

        Mat alpha = chans.get(3);

        Mat mask = new Mat();
        Imgproc.threshold(alpha, mask, 0, 255, Imgproc.THRESH_BINARY_INV);

        Mat bgr = new Mat();
        Core.merge(Arrays.asList(chans.get(0), chans.get(1), chans.get(2)), bgr);

        bgr.setTo(bgColorBGR, mask);

        return bgr;
    }
}
