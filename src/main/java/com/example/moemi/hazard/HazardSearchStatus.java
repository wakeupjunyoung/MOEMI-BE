package com.example.moemi.hazard;

/**
 * Lets the app tell "nothing nearby" apart from errors and from areas without data.
 */
public enum HazardSearchStatus {
    OK,
    /** Searched successfully, nothing found ("주변에 등록된 정보가 없습니다"). */
    EMPTY,
    /** Outside the area the source covers ("이 지역은 아직 지원하지 않습니다"). */
    UNSUPPORTED_REGION,
    /** The source could not be reached ("불러오지 못함 · 다시 시도"). */
    FAILED
}
