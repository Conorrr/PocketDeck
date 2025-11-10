package io.restall.sharedex.classifier.opencv;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class CardFeatureMarshaller {

    @SneakyThrows
    public static void write(Map<String, CardFeatures> cardDatabase, Path databaseFile) {
        if (!Files.exists(databaseFile.toAbsolutePath().getParent())) {
            Files.createDirectories(databaseFile.getParent());
        }

        try (DataOutputStream dos = new DataOutputStream(Files.newOutputStream(databaseFile))) {
            dos.writeInt(cardDatabase.size());
            for (Map.Entry<String, CardFeatures> entry : cardDatabase.entrySet()) {
                String cardName = entry.getKey();
                CardFeatures cf = entry.getValue();
                dos.writeUTF(cardName);
                writeMat(dos, cf.descriptors());
                writeKeypoints(dos, cf.keypoints());
            }
        }
    }

    @SneakyThrows
    private static void writeMat(DataOutputStream dos, Mat mat) {
        int rows = mat.rows();
        int cols = mat.cols();
        int type = mat.type();
        int channels = mat.channels();

        // Safe handling of empty or invalid Mats
        if (mat.empty() || rows <= 0 || cols <= 0) {
            rows = 1;
            cols = 1;
            mat = Mat.zeros(1, 1, type); // placeholder
        }

        dos.writeInt(rows);
        dos.writeInt(cols);
        dos.writeInt(type);
        dos.writeInt(channels);

        int totalElements = rows * cols * channels;

        if (type == CvType.CV_8UC1 || type == CvType.CV_8UC3) {
            byte[] data = new byte[totalElements];
            mat.get(0, 0, data);
            dos.write(data);
        } else if (type == CvType.CV_32FC1 ||
                type == CvType.CV_32FC2 ||
                type == CvType.CV_32FC3 ||
                type == CvType.CV_32FC4) {
            float[] data = new float[totalElements];
            mat.get(0, 0, data);
            for (float v : data) dos.writeFloat(v);
        } else {
            throw new IllegalArgumentException("Unsupported Mat type: " + type);
        }
    }


    @SneakyThrows
    private static void writeKeypoints(DataOutputStream dos, MatOfKeyPoint keypoints) {
        Mat mat = keypointsToMat(keypoints);
        writeMat(dos, mat);
    }

    private static Mat keypointsToMat(MatOfKeyPoint keypoints) {
        KeyPoint[] kpArray = keypoints.toArray();
        int n = kpArray.length;
        Mat mat = new Mat(n, 1, CvType.CV_32FC2); // 1 column, 2 channels
        float[] data = new float[n * 2];
        for (int i = 0; i < n; i++) {
            data[i * 2] = (float) kpArray[i].pt.x;
            data[i * 2 + 1] = (float) kpArray[i].pt.y;
        }
        mat.put(0, 0, data);
        return mat;
    }

    @SneakyThrows
    public static Map<String, CardFeatures> read(Path databaseFile) {
        try (DataInputStream dis = new DataInputStream(Files.newInputStream(databaseFile))) {
            var len = dis.readInt();
            var cards = new HashMap<String, CardFeatures>(len);
            for (int i = 0; i < len; i++) {
                var cardName = dis.readUTF();
                var descriptors = readMat(dis);
                var keyPoints = readKeypoints(dis);
                cards.put(cardName, new CardFeatures(cardName, keyPoints, descriptors));
            }
            return cards;
        }
    }

    @SneakyThrows
    private static Mat readMat(DataInputStream dis) {
        int rows = dis.readInt();
        int cols = dis.readInt();
        int type = dis.readInt();
        int channels = dis.readInt();

        if (rows <= 0 || cols <= 0) {
            // placeholder empty Mat
            return Mat.zeros(1, 1, type);
        }

        Mat mat = new Mat(rows, cols, type);
        int totalElements = rows * cols * channels;

        if (type == CvType.CV_8UC1 || type == CvType.CV_8UC3) {
            byte[] data = new byte[totalElements];
            dis.readFully(data);
            mat.put(0, 0, data);
        } else if (type == CvType.CV_32FC1 ||
                type == CvType.CV_32FC2 ||
                type == CvType.CV_32FC3 ||
                type == CvType.CV_32FC4) {
            float[] data = new float[totalElements];
            for (int i = 0; i < totalElements; i++) data[i] = dis.readFloat();
            mat.put(0, 0, data);
        } else {
            throw new IllegalArgumentException("Unsupported Mat type: " + type);
        }

        return mat;
    }

    @SneakyThrows
    private static MatOfKeyPoint readKeypoints(DataInputStream dis) {
        Mat mat = readMat(dis); // Nx1, 2 channels
        int n = mat.rows();
        KeyPoint[] kpArray = new KeyPoint[n];

        float[] data = new float[n * 2];
        mat.get(0, 0, data);

        for (int i = 0; i < n; i++) {
            kpArray[i] = new KeyPoint(data[i * 2], data[i * 2 + 1], 1f);
        }

        MatOfKeyPoint keypoints = new MatOfKeyPoint();
        keypoints.fromArray(kpArray);
        return keypoints;
    }

}
