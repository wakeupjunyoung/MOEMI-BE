package com.example.moemi.schedule.travel;

/**
 * Whether a travel time could be estimated. Whatever the result, the schedule can still be saved
 * with a travel time the user enters.
 */
public enum TravelStatus {
    OK,
    /** The place has no coordinates yet; the app picks or geocodes it first ("장소 위치를 선택해 주세요"). */
    NO_DESTINATION,
    /** The app did not send where the user starts from ("출발 위치를 알 수 없습니다"). */
    NO_ORIGIN,
    /** The route service could not be reached, has no key, or found no route ("이동 시간 계산 불가"). */
    UNAVAILABLE
}
