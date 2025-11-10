package io.restall.sharedex.classifier;


import org.opencv.core.Mat;
import org.opencv.core.Rect;

public class SafetyMat extends Mat implements AutoCloseable {

    public SafetyMat() {
        super();
    }

    public SafetyMat(Mat m, Rect roi) {
        super(m, roi);
    }

    @Override
    public void close() {
        this.release();
    }
}