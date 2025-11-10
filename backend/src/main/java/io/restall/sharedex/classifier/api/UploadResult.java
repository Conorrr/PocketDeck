package io.restall.sharedex.classifier.api;

import io.restall.sharedex.classifier.opencv.Prediction;

import java.util.List;

public record UploadResult(String deckId, String uploadId, int totalCards, List<Prediction> cards) {
}
