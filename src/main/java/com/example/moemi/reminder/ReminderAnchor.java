package com.example.moemi.reminder;

/**
 * What a reminder's "minutes before" counts from.
 */
public enum ReminderAnchor {
    /** Before leaving: the start minus travel time ("출발 1시간 전"). Same as START without travel time. */
    DEPARTURE("출발"),
    /** Before the schedule starts ("시작 1시간 전"). */
    START("시작");

    private final String label;

    ReminderAnchor(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
