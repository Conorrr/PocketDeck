package io.restall.sharedex.classifier.collections;

import java.util.List;

public record AttackDetails(
        List<String> cost,
        String name,
        String damage,
        String effect
) {
}
