package com.example.moemi.transit;

/**
 * State of real-time information (arrivals, running buses). Whatever the state, favorites and
 * static route information are still shown; only the real-time part changes.
 */
public enum RealtimeStatus {
    /** Buses are on their way or running. */
    AVAILABLE,
    /** Looked up successfully, but no bus is coming or running now ("도착 예정 버스 없음"), e.g. at night. */
    NO_BUS,
    /** The city has no real-time information ("이 지역은 실시간 정보를 제공하지 않습니다"). */
    NOT_SUPPORTED,
    /** The route is gone from TAGO ("운행 정보 없음 · 노선 변경 확인 필요"). */
    ROUTE_NOT_FOUND,
    /** TAGO could not be reached ("실시간 정보를 불러올 수 없습니다"). */
    FAILED
}
