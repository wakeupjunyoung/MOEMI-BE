package com.example.moemi.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class ScheduleConflictTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 29);

    @Test
    void overlappingSchedulesAreReported() {
        Occurrence meeting = occurrence("미팅", 14, 0, LocalTime.of(15, 0), null, null);
        Occurrence call = occurrence("통화", 14, 30, LocalTime.of(14, 45), null, null);

        assertThat(ScheduleConflict.find(List.of(call, meeting)))
                .singleElement()
                .satisfies(conflict -> {
                    assertThat(conflict.type()).isEqualTo(ScheduleConflict.Type.OVERLAP);
                    assertThat(conflict.earlier()).isSameAs(meeting);
                });
    }

    @Test
    void schedulesWithoutEndOnlyConflictAtTheSameStart() {
        Occurrence a = occurrence("A", 14, 0, null, null, null);
        Occurrence b = occurrence("B", 14, 30, null, null, null);
        Occurrence c = occurrence("C", 14, 0, null, null, null);

        assertThat(ScheduleConflict.find(List.of(a, b))).isEmpty();
        assertThat(ScheduleConflict.find(List.of(a, c))).extracting(ScheduleConflict::type)
                .containsExactly(ScheduleConflict.Type.OVERLAP);
    }

    @Test
    void preparationStartingBeforeThePreviousEndIsNotEnoughTime() {
        Occurrence lunch = occurrence("점심", 12, 0, LocalTime.of(13, 0), null, null);
        // 14:00 start - 40 min travel - 30 min prep = 12:50, before lunch ends.
        Occurrence meeting = occurrence("미팅", 14, 0, LocalTime.of(15, 0), 30, 40);

        assertThat(ScheduleConflict.find(List.of(lunch, meeting))).extracting(ScheduleConflict::type)
                .containsExactly(ScheduleConflict.Type.NOT_ENOUGH_TIME);
        assertThat(meeting.departAt()).isEqualTo(LocalDateTime.of(DAY, LocalTime.of(13, 20)));
        assertThat(meeting.prepStartAt()).isEqualTo(LocalDateTime.of(DAY, LocalTime.of(12, 50)));
    }

    @Test
    void repeatingSchedulesOccurOnMatchingDatesUntilTheEnd() {
        Schedule weekly = schedule("수영", 7, 0, null, null, null, RepeatType.WEEKLY, LocalDate.of(2026, 11, 12));
        Schedule monthly = schedule("월세", 9, 0, null, null, null, RepeatType.MONTHLY, null);

        assertThat(weekly.occursOn(LocalDate.of(2026, 11, 5))).isTrue();
        assertThat(weekly.occursOn(LocalDate.of(2026, 11, 6))).isFalse();
        assertThat(weekly.occursOn(LocalDate.of(2026, 11, 19))).isFalse();
        assertThat(weekly.occursOn(LocalDate.of(2026, 10, 22))).isFalse();
        assertThat(monthly.occursOn(LocalDate.of(2027, 1, 29))).isTrue();
        assertThat(monthly.occursOn(LocalDate.of(2027, 2, 28))).isFalse();
    }

    private static Occurrence occurrence(String title, int hour, int minute, LocalTime end, Integer prep, Integer travel) {
        return new Occurrence(schedule(title, hour, minute, end, prep, travel, RepeatType.NONE, null), DAY);
    }

    private static Schedule schedule(String title, int hour, int minute, LocalTime end, Integer prep, Integer travel,
            RepeatType repeat, LocalDate until) {
        return Schedule.create("device", new ScheduleDraft(title, DAY, LocalTime.of(hour, minute), end, null, null, null,
                null, repeat, until, prep, travel, null), LocalDateTime.of(DAY, LocalTime.NOON));
    }
}
