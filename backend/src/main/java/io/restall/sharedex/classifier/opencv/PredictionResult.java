package io.restall.sharedex.classifier.opencv;

import java.util.List;
import java.util.Optional;

public record PredictionResult(List<Prediction> topPredictions) {

    public Optional<Prediction> getBestMatch() {
        return topPredictions.isEmpty() ? Optional.empty() : Optional.of(topPredictions.getFirst());
    }

    public List<Prediction> getTopMatches() {
        return topPredictions;
    }

    public boolean isConfident(double minScore) {
        var best = getBestMatch();
        if (best.isEmpty()) {
            return false;
        }

        // Check if best match is significantly better than second best
        if (topPredictions.size() > 1) {
            double scoreGap = best.get().confidence() - topPredictions.get(1).confidence();
            return best.get().confidence() >= minScore && scoreGap > 10.0;
        }

        return best.get().confidence() >= minScore;
    }

    @Override
    public String toString() {
        if (topPredictions.isEmpty()) {
            return "No matches found";
        }

        StringBuilder sb = new StringBuilder("Top matches:");
        for (int i = 0; i < topPredictions.size(); i++) {
            sb.append(String.format("  %d. %s\t", i + 1, topPredictions.get(i)));
        }
        return sb.toString();
    }
}