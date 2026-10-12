package com.example.moemi.checklist;

import java.util.List;

/**
 * @param items the recommendations; empty unless {@code status} is OK
 */
public record RecommendationResult(RecommendationStatus status, List<Recommendation> items) {

    public static RecommendationResult ok(List<Recommendation> items) {
        return new RecommendationResult(RecommendationStatus.OK, List.copyOf(items));
    }

    public static RecommendationResult noData() {
        return new RecommendationResult(RecommendationStatus.NO_DATA, List.of());
    }

    public static RecommendationResult failed() {
        return new RecommendationResult(RecommendationStatus.FAILED, List.of());
    }
}
