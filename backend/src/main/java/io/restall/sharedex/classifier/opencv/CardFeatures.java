package io.restall.sharedex.classifier.opencv;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

public record CardFeatures(String cardName, MatOfKeyPoint keypoints, Mat descriptors) {
}