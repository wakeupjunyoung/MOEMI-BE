package com.example.moemi.checklist;

public enum RecommendationStatus {
    /** Recommendations reflect the date's life indices (possibly none were needed). */
    OK,
    /** The date is outside the forecast range, so nothing could be recommended. */
    NO_DATA,
    /** Life indices could not be loaded ("추천 정보를 불러오지 못했습니다"). */
    FAILED
}
