package com.example.moemi.reminder;

import java.util.Comparator;
import java.util.List;

import com.example.moemi.common.error.ApiException;

/**
 * Validates "minutes before the start" lists: at most {@value #MAX_COUNT} of them, each from 0
 * (at the start) to {@value #MAX_MINUTES} (one week), stored largest first without duplicates.
 */
public final class ReminderOffsets {

    public static final int MAX_COUNT = 5;
    public static final int MAX_MINUTES = 7 * 24 * 60;

    private ReminderOffsets() {
    }

    /** @return the offsets largest first; empty for {@code null} or an empty list */
    public static List<Integer> normalize(List<Integer> offsets) {
        if (offsets == null || offsets.isEmpty()) {
            return List.of();
        }
        List<Integer> distinct = offsets.stream().distinct().sorted(Comparator.reverseOrder()).toList();
        if (distinct.size() > MAX_COUNT) {
            throw ApiException.badRequest("INVALID_REMINDER", "알림 시점은 최대 " + MAX_COUNT + "개까지 설정할 수 있습니다.");
        }
        for (Integer minutes : distinct) {
            if (minutes == null || minutes < 0 || minutes > MAX_MINUTES) {
                throw ApiException.badRequest("INVALID_REMINDER", "알림 시점은 0분에서 " + MAX_MINUTES + "분(7일) 전 사이여야 합니다.");
            }
        }
        return distinct;
    }

    /** "출발 1시간 30분 전", "시작 10분 전", "지금 출발". */
    public static String phrase(ReminderAnchor anchor, int minutesBefore) {
        if (minutesBefore == 0) {
            return "지금 " + anchor.getLabel();
        }
        return anchor.getLabel() + " " + label(minutesBefore);
    }

    /** "1시간 30분 전", "10분 전"; for a positive number of minutes. */
    static String label(int minutesBefore) {
        int days = minutesBefore / (24 * 60);
        int hours = minutesBefore % (24 * 60) / 60;
        int minutes = minutesBefore % 60;
        StringBuilder label = new StringBuilder();
        if (days > 0) {
            label.append(days).append("일 ");
        }
        if (hours > 0) {
            label.append(hours).append("시간 ");
        }
        if (minutes > 0) {
            label.append(minutes).append("분 ");
        }
        return label.append("전").toString();
    }
}
