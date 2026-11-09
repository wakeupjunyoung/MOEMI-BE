package com.example.moemi.schedule.parse;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class ScheduleTextParserTest {

    /** A Thursday. */
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);

    @Test
    void readsDateTimePlaceAndTitle() {
        ParsedSchedule parsed = ScheduleTextParser.parse("내일 오후 2시에 강남에서 미팅", TODAY);

        assertThat(parsed.date()).isEqualTo(LocalDate.of(2026, 10, 30));
        assertThat(parsed.startTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(parsed.place()).isEqualTo("강남");
        assertThat(parsed.title()).isEqualTo("미팅");
        assertThat(parsed.missingFields()).isEmpty();
    }

    @Test
    void timeRangeSharesTheMeridiemAndNextWeekdayIsInNextWeek() {
        ParsedSchedule parsed = ScheduleTextParser.parse("다음주 월요일 오후 2시부터 4시까지 팀 회의", TODAY);

        assertThat(parsed.date()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(parsed.startTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(parsed.endTime()).isEqualTo(LocalTime.of(16, 0));
        assertThat(parsed.title()).isEqualTo("팀 회의");
    }

    @Test
    void durationSetsTheEndTime() {
        ParsedSchedule parsed = ScheduleTextParser.parse("금요일 저녁 7시 홍대에서 저녁 약속 2시간", TODAY);

        assertThat(parsed.date()).isEqualTo(LocalDate.of(2026, 10, 30));
        assertThat(parsed.startTime()).isEqualTo(LocalTime.of(19, 0));
        assertThat(parsed.endTime()).isEqualTo(LocalTime.of(21, 0));
        assertThat(parsed.place()).isEqualTo("홍대");
        assertThat(parsed.title()).isEqualTo("저녁 약속");
    }

    @Test
    void timeWithoutMeridiemIsLeftEmptyWithOptions() {
        ParsedSchedule parsed = ScheduleTextParser.parse("11월 3일 3시 반 치과", TODAY);

        assertThat(parsed.date()).isEqualTo(LocalDate.of(2026, 11, 3));
        assertThat(parsed.startTime()).isNull();
        assertThat(parsed.startTimeOptions()).containsExactly(LocalTime.of(3, 30), LocalTime.of(15, 30));
        assertThat(parsed.missingFields()).containsExactly("startTime");
        assertThat(parsed.title()).isEqualTo("치과");
    }

    @Test
    void twentyFourHourTimeIsNotAmbiguous() {
        ParsedSchedule parsed = ScheduleTextParser.parse("모레 14:30 병원", TODAY);

        assertThat(parsed.date()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(parsed.startTime()).isEqualTo(LocalTime.of(14, 30));
        assertThat(parsed.title()).isEqualTo("병원");
    }

    @Test
    void pastMonthDayMeansNextYear() {
        assertThat(ScheduleTextParser.parse("3월 1일 오전 9시 입학식", TODAY).date()).isEqualTo(LocalDate.of(2027, 3, 1));
    }

    @Test
    void durationIsNotReadAsAStartTime() {
        ParsedSchedule parsed = ScheduleTextParser.parse("회의 2시간 내일 오전 10시", TODAY);

        assertThat(parsed.startTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(parsed.endTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(parsed.title()).isEqualTo("회의");
    }

    @Test
    void nothingRecognizedLeavesFieldsEmptyInsteadOfGuessing() {
        ParsedSchedule parsed = ScheduleTextParser.parse("회의", TODAY);

        assertThat(parsed.title()).isEqualTo("회의");
        assertThat(parsed.date()).isNull();
        assertThat(parsed.startTime()).isNull();
        assertThat(parsed.missingFields()).containsExactly("date", "startTime");
        assertThat(parsed.notes()).isNotEmpty();
    }
}
