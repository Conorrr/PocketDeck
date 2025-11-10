package io.restall.sharedex.classifier.opencv;

public record Prediction(String cardName, double hashScore, int matchCount, double confidence) {

    @Override
    public String toString() {
        return String.format("%s (score: %.1f, matches: %d)",
                cardName, confidence, matchCount);
    }
}