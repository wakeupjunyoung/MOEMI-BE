package com.example.moemi.transit;

/**
 * Result of a stop or route search.
 */
public enum SearchStatus {
    OK,
    /** Searched successfully, nothing found ("검색 결과가 없습니다"); see the suggestions. */
    EMPTY,
    /** TAGO could not be reached ("검색하지 못했습니다 · 다시 시도"). */
    FAILED
}
