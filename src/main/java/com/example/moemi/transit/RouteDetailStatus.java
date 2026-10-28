package com.example.moemi.transit;

/**
 * How the route detail was obtained.
 */
public enum RouteDetailStatus {
    OK,
    /** TAGO could not be reached, so the last fetched route is returned ("최신 정보 아님"). */
    STALE,
    /** TAGO no longer knows the route ("운행 정보 없음 · 노선 변경 확인 필요"). */
    NOT_FOUND,
    /** TAGO could not be reached and nothing was cached ("불러오지 못함 · 다시 시도"). */
    FAILED
}
