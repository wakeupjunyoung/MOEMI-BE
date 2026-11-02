package com.example.moemi.schedule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Two occurrences that do not fit together on a day.
 *
 * @param earlier the one that starts first
 * @param later   the one that starts at the same time or after
 */
public record ScheduleConflict(Type type, Occurrence earlier, Occurrence later) {

    public enum Type {
        /** The schedules themselves overlap ("일정이 겹칩니다"). */
        OVERLAP,
        /** The later one's preparation or travel starts before the earlier one ends ("준비·이동 시간이 부족합니다"). */
        NOT_ENOUGH_TIME
    }

    /** Every conflicting pair among the occurrences of one day. */
    public static List<ScheduleConflict> find(List<Occurrence> occurrences) {
        List<Occurrence> sorted = occurrences.stream().sorted(Comparator.comparing(Occurrence::startAt)).toList();
        List<ScheduleConflict> conflicts = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            for (int j = i + 1; j < sorted.size(); j++) {
                Occurrence earlier = sorted.get(i);
                Occurrence later = sorted.get(j);
                if (overlaps(earlier, later)) {
                    conflicts.add(new ScheduleConflict(Type.OVERLAP, earlier, later));
                } else if (later.prepStartAt().isBefore(earlier.effectiveEnd())) {
                    conflicts.add(new ScheduleConflict(Type.NOT_ENOUGH_TIME, earlier, later));
                }
            }
        }
        return conflicts;
    }

    public boolean involves(Occurrence occurrence) {
        return earlier == occurrence || later == occurrence;
    }

    /** {@code earlier} starts no later than {@code later}. A schedule without an end only covers its start. */
    private static boolean overlaps(Occurrence earlier, Occurrence later) {
        return earlier.startAt().equals(later.startAt()) || later.startAt().isBefore(earlier.effectiveEnd());
    }
}
