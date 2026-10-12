package com.example.moemi.hazard.its;

import java.time.LocalDateTime;

import com.example.moemi.hazard.GeoPoint;

/**
 * One incident from the ITS event API (돌발상황정보), as reported. Blank fields are {@code null}.
 *
 * @param roadType     고속도로 / 국도 / 지방도 / 시군도 / 기타
 * @param eventType    e.g. 공사, 작업, 교통사고, 기상, 재난, 기타돌발
 * @param direction    roadDrcType, e.g. 상행 / 하행
 * @param blockType    lanesBlockType, e.g. 전면통제 / 차로부분통제
 * @param blockedLanes lanesBlocked, e.g. "2차로 차단"
 * @param message      the original description
 */
public record ItsEvent(
        String roadType,
        String eventType,
        String eventDetailType,
        LocalDateTime startAt,
        LocalDateTime endAt,
        GeoPoint location,
        String linkId,
        String roadName,
        String roadNo,
        String direction,
        String blockType,
        String blockedLanes,
        String message) {
}
