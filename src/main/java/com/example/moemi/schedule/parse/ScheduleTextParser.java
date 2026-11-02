package com.example.moemi.schedule.parse;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a Korean schedule sentence such as "내일 오후 2시에 강남에서 미팅" into its parts.
 * Rule-based on purpose: it never invents a value. A time without 오전/오후 is reported as
 * options, and anything it cannot find is listed as missing for the user to fill in.
 */
public final class ScheduleTextParser {

    private static final String MERIDIEM = "(오전|오후|아침|점심|저녁|밤|새벽)";
    /** "2시", "2시 30분", "2시 반", "14:30", optionally after a meridiem word. */
    private static final String TIME = "(?:" + MERIDIEM + "\\s*)?(\\d{1,2})(?:\\s*시(?!간)(?:\\s*(\\d{1,2})\\s*분|\\s*(반))?|:(\\d{2}))";
    private static final int TIME_GROUPS = 5;

    private static final Pattern TIME_RANGE = Pattern.compile(TIME + "\\s*(?:부터|~|-)\\s*" + TIME + "\\s*(?:까지)?(?:에)?");
    private static final Pattern SINGLE_TIME = Pattern.compile(TIME + "(?:에|부터)?");
    private static final Pattern DURATION = Pattern.compile("(\\d{1,2})\\s*시간(?:\\s*(반))?(?:\\s*(?:동안|정도))?");

    private static final Pattern FULL_DATE = Pattern.compile("(\\d{4})\\s*[.\\-/년]\\s*(\\d{1,2})\\s*[.\\-/월]\\s*(\\d{1,2})\\s*일?(?:에)?");
    private static final Pattern MONTH_DAY = Pattern.compile("(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일(?:에)?");
    private static final Pattern SLASH_DATE = Pattern.compile("(?<![\\d:])(\\d{1,2})/(\\d{1,2})(?![\\d:])(?:에)?");
    private static final Pattern RELATIVE_DAY = Pattern.compile("(오늘|내일|모레|글피)(?:에)?");
    private static final Pattern WEEKDAY = Pattern.compile("(?:(이번\\s*주|다음\\s*주|담주|다다음\\s*주)\\s*)?([월화수목금토일])요일(?:에)?");
    private static final Pattern DAY_ONLY = Pattern.compile("(?<!\\d)(\\d{1,2})\\s*일(?!\\s*(?:간|동안))(?:에)?");

    private static final Pattern PLACE = Pattern.compile("(\\S+?)에서(?=\\s|$)");
    private static final Pattern LEFTOVER_PARTICLES = Pattern.compile("(?<=^|\\s)(?:에|에서|부터|까지|~|-|,)(?=\\s|$)");

    private static final Map<String, Integer> RELATIVE_OFFSETS = Map.of("오늘", 0, "내일", 1, "모레", 2, "글피", 3);
    private static final String WEEKDAYS = "월화수목금토일";

    private ScheduleTextParser() {
    }

    public static ParsedSchedule parse(String text, LocalDate today) {
        Working working = new Working(text.trim().replaceAll("\\s+", " "));
        List<String> notes = new ArrayList<>();

        LocalDate date = parseDate(working, today);

        LocalTime start = null;
        LocalTime end = null;
        List<LocalTime> startOptions = List.of();
        Matcher range = TIME_RANGE.matcher(working.text);
        if (range.find()) {
            Reading startReading = reading(range, 1, null);
            Reading endReading = reading(range, 1 + TIME_GROUPS, startReading.meridiem());
            start = startReading.time();
            startOptions = startReading.options();
            end = endReading.time() != null ? endReading.time() : laterOption(endReading.options(), start);
            working.consume(range);
        } else {
            Matcher single = SINGLE_TIME.matcher(working.text);
            if (single.find()) {
                Reading reading = reading(single, 1, null);
                start = reading.time();
                startOptions = reading.options();
                working.consume(single);
            }
        }
        if (start == null && !startOptions.isEmpty()) {
            notes.add("오전인지 오후인지 확인해 주세요.");
        }

        Matcher duration = DURATION.matcher(working.text);
        if (duration.find()) {
            if (start != null && end == null) {
                int minutes = Integer.parseInt(duration.group(1)) * 60 + (duration.group(2) != null ? 30 : 0);
                LocalTime computed = start.plusMinutes(minutes);
                if (computed.isAfter(start)) {
                    end = computed;
                } else {
                    notes.add("종료 시각이 다음 날로 넘어가 비워 두었습니다.");
                }
            }
            working.consume(duration);
        }
        if (end != null && start != null && !end.isAfter(start)) {
            notes.add("종료 시각이 시작 시각보다 빨라 비워 두었습니다.");
            end = null;
        }

        String place = null;
        Matcher placeMatcher = PLACE.matcher(working.text);
        if (placeMatcher.find()) {
            place = placeMatcher.group(1);
            working.consume(placeMatcher);
        }

        String title = LEFTOVER_PARTICLES.matcher(working.text).replaceAll(" ").replaceAll("\\s+", " ").trim();
        if (title.isEmpty()) {
            title = null;
        }

        List<String> missing = new ArrayList<>();
        if (title == null) {
            missing.add("title");
        }
        if (date == null) {
            missing.add("date");
        }
        if (start == null) {
            missing.add("startTime");
        }
        if (!missing.isEmpty()) {
            notes.add("인식하지 못한 항목은 직접 입력해 주세요.");
        }
        return new ParsedSchedule(title, date, start, end, place, startOptions, List.copyOf(missing), List.copyOf(notes));
    }

    private static LocalDate parseDate(Working working, LocalDate today) {
        Matcher m = FULL_DATE.matcher(working.text);
        if (m.find()) {
            LocalDate date = safeDate(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
            working.consume(m);
            return date;
        }
        m = MONTH_DAY.matcher(working.text);
        if (m.find()) {
            LocalDate date = upcoming(today, Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
            working.consume(m);
            return date;
        }
        m = SLASH_DATE.matcher(working.text);
        if (m.find()) {
            LocalDate date = upcoming(today, Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
            working.consume(m);
            return date;
        }
        m = RELATIVE_DAY.matcher(working.text);
        if (m.find()) {
            working.consume(m);
            return today.plusDays(RELATIVE_OFFSETS.get(m.group(1)));
        }
        m = WEEKDAY.matcher(working.text);
        if (m.find()) {
            DayOfWeek day = DayOfWeek.of(WEEKDAYS.indexOf(m.group(2)) + 1);
            String week = m.group(1) == null ? "" : m.group(1).replace(" ", "");
            working.consume(m);
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return switch (week) {
                case "다음주", "담주" -> monday.plusWeeks(1).with(TemporalAdjusters.nextOrSame(day));
                case "다다음주" -> monday.plusWeeks(2).with(TemporalAdjusters.nextOrSame(day));
                case "이번주" -> monday.with(TemporalAdjusters.nextOrSame(day));
                default -> today.with(TemporalAdjusters.nextOrSame(day));
            };
        }
        m = DAY_ONLY.matcher(working.text);
        if (m.find()) {
            int day = Integer.parseInt(m.group(1));
            LocalDate date = safeDate(today.getYear(), today.getMonthValue(), day);
            if (date != null && date.isBefore(today)) {
                LocalDate next = today.plusMonths(1);
                date = safeDate(next.getYear(), next.getMonthValue(), day);
            }
            working.consume(m);
            return date;
        }
        return null;
    }

    /** The next such month/day from today on: a date already past this year means next year. */
    private static LocalDate upcoming(LocalDate today, int month, int day) {
        LocalDate date = safeDate(today.getYear(), month, day);
        if (date != null && date.isBefore(today)) {
            date = safeDate(today.getYear() + 1, month, day);
        }
        return date;
    }

    private static LocalDate safeDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            return null;
        }
    }

    /**
     * @param first        index of the time's first group (the meridiem)
     * @param fallbackWord meridiem to use when this time has none, e.g. "오후 2시부터 4시까지"
     */
    private static Reading reading(Matcher m, int first, String fallbackWord) {
        String word = m.group(first) != null ? m.group(first) : fallbackWord;
        int hour = Integer.parseInt(m.group(first + 1));
        int minute = 0;
        if (m.group(first + 2) != null) {
            minute = Integer.parseInt(m.group(first + 2));
        } else if (m.group(first + 3) != null) {
            minute = 30;
        } else if (m.group(first + 4) != null) {
            minute = Integer.parseInt(m.group(first + 4));
        }
        if (hour > 24 || minute > 59) {
            return new Reading(null, List.of(), word);
        }
        if (word == null) {
            if (hour == 0 || hour >= 13) {
                return new Reading(LocalTime.of(hour % 24, minute), List.of(), null);
            }
            // "3시" could be 03:00 or 15:00; let the user choose.
            return new Reading(null, List.of(LocalTime.of(hour % 12, minute), LocalTime.of(hour % 12 + 12, minute)), null);
        }
        return new Reading(LocalTime.of(withMeridiem(word, hour) % 24, minute), List.of(), word);
    }

    private static int withMeridiem(String word, int hour) {
        return switch (word) {
            case "오전", "아침", "새벽" -> hour == 12 ? 0 : hour;
            case "점심" -> hour <= 4 ? hour + 12 : hour;
            case "밤" -> hour == 12 ? 0 : hour < 12 ? hour + 12 : hour;
            default -> hour < 12 ? hour + 12 : hour; // 오후, 저녁, 밤
        };
    }

    /** For an ambiguous end time, the reading that comes after the start. */
    private static LocalTime laterOption(List<LocalTime> options, LocalTime start) {
        if (start == null) {
            return null;
        }
        return options.stream().filter(option -> option.isAfter(start)).findFirst().orElse(null);
    }

    private record Reading(LocalTime time, List<LocalTime> options, String meridiem) {
    }

    /** The sentence with recognized parts blanked out, so the rest becomes the title. */
    private static final class Working {

        private String text;

        Working(String text) {
            this.text = text;
        }

        void consume(Matcher m) {
            text = (text.substring(0, m.start()) + " " + text.substring(m.end())).replaceAll("\\s+", " ").trim();
        }
    }
}
