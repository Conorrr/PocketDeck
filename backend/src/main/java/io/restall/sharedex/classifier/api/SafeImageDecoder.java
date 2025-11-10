package io.restall.sharedex.classifier.api;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.*;

public final class SafeImageDecoder {

    private final ExecutorService ex = Executors.newFixedThreadPool(5, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    public static class DecodeException extends Exception {
        public DecodeException(String msg) {
            super(msg);
        }

        public DecodeException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    public Mat safeDecode(
            byte[] bytes,
            long maxBytes,
            int maxWidth,
            int maxHeight,
            long maxPixels,
            long timeoutMs,
            int flags
    ) throws DecodeException {

        if (bytes == null || bytes.length == 0) {
            throw new DecodeException("Empty input bytes");
        }

        if (bytes.length > maxBytes) {
            throw new DecodeException("Encoded image too large: " + bytes.length + " bytes (max " + maxBytes + ")");
        }

        // 1) Try to get dimensions via ImageIO (works for PNG/JPEG/etc)
        Dimension dim = null;
        try {
            dim = readImageDimensionsWithImageIO(bytes);
        } catch (IOException ignored) {
            // We'll fall back to safe decode below
        }

        // 2) If we have dimensions, enforce them early
        if (dim != null) {
            if (dim.width <= 0 || dim.height <= 0) {
                throw new DecodeException("Invalid image dimensions: " + dim.width + "x" + dim.height);
            }
            if (dim.width > maxWidth || dim.height > maxHeight) {
                throw new DecodeException("Image dimensions exceed limits: " + dim.width + "x" + dim.height);
            }
            if ((long) dim.width * dim.height > maxPixels) {
                throw new DecodeException("Image pixel count too large: " + ((long) dim.width * dim.height));
            }
        }


        Future<Mat> future = ex.submit(() -> {
            var buf = new MatOfByte(bytes);
            try {
                return Imgcodecs.imdecode(buf, flags);
            } finally {
                buf.release();
            }
        });

        try {
            Mat decoded = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (decoded == null || decoded.empty()) {
                if (decoded != null) decoded.release();
                throw new DecodeException("Decoded Mat is empty or null");
            }

            // Post-check dimensions & memory estimate
            int w = decoded.cols();
            int h = decoded.rows();
            int elemSize = (int) decoded.elemSize(); // bytes per pixel
            long pixels = (long) w * h;

            if (w <= 0 || h <= 0) {
                decoded.release();
                throw new DecodeException("Decoded image has invalid size: " + w + "x" + h);
            }
            if (w > maxWidth || h > maxHeight || pixels > maxPixels) {
                decoded.release();
                throw new DecodeException("Decoded image too large after decode: " + w + "x" + h);
            }

            // Estimate memory footprint (approx)
            long bytesNeeded = pixels * elemSize;
            // Put a hard upper safety too
            long hardMaxBytes = Math.max(maxBytes, 200_000_000L); // e.g., 200MB absolute ceiling
            if (bytesNeeded > hardMaxBytes) {
                decoded.release();
                throw new DecodeException("Decoded image would allocate too much memory: ~" + bytesNeeded + " bytes");
            }

            // Looks good — return the Mat; caller must release it
            return decoded;

        } catch (TimeoutException te) {
            future.cancel(true);
            throw new DecodeException("Timed out while decoding image", te);
        } catch (ExecutionException ee) {
            throw new DecodeException("Error during decoding", ee.getCause());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new DecodeException("Decoding interrupted", ie);
        }
    }

    // Small helper to read dimensions via ImageIO without decoding entire image
    private static Dimension readImageDimensionsWithImageIO(byte[] bytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ImageInputStream iis = ImageIO.createImageInputStream(bais)) {

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                return new Dimension(width, height);
            } finally {
                reader.dispose();
            }
        }
    }

    private static final class Dimension {
        final int width;
        final int height;

        Dimension(int w, int h) {
            this.width = w;
            this.height = h;
        }
    }
}
