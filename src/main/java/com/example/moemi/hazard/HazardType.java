package com.example.moemi.hazard;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HazardType {
    CONSTRUCTION("공사"),
    CONTROL("통제"),
    ACCIDENT("사고"),
    /** Weather, disaster and other incidents the source does not classify further. */
    OTHER("기타");

    private final String label;

    /**
     * Classifies an ITS incident. ITS reports 공사/작업, 교통사고, 기상, 재난, 기타돌발 and 기타 as event types
     * and lane closures separately, so a closure only decides the type when nothing more specific does.
     */
    public static HazardType classify(String eventType, String blockType, String blockedLanes) {
        String type = eventType == null ? "" : eventType;
        if (type.contains("사고")) {
            return ACCIDENT;
        }
        if (type.contains("공사") || type.contains("작업")) {
            return CONSTRUCTION;
        }
        if (blockType != null || blockedLanes != null) {
            return CONTROL;
        }
        return OTHER;
    }
}
