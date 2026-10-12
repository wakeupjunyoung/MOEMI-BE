package com.example.moemi.lifeindex;

/**
 * How fresh the returned indices are.
 */
public enum LifeIndexStatus {
    /** Fetched within the cache lifetime. */
    OK,
    /** KMA could not be reached, so the last fetched values are returned ("최신 데이터 아님"). */
    STALE,
    /** KMA could not be reached and nothing was cached ("조회 실패"). */
    FAILED
}
