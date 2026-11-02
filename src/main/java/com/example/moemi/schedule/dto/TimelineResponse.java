package com.example.moemi.schedule.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.example.moemi.schedule.ChecklistLinks;
import com.example.moemi.schedule.Occurrence;
import com.example.moemi.schedule.RepeatType;
import com.example.moemi.schedule.ScheduleConflict;
import com.example.moemi.schedule.travel.TravelMode;

/**
 * One day in time order, for the timeline screen.
 *
 * @param blocks    what to draw: preparation, travel and the schedule itself, each as its own block
 * @param conflicts overlaps and too-tight gaps to warn about
 */
public record TimelineResponse(
        LocalDate date,
        List<TimelineSchedule> schedules,
        List<Block> blocks,
        List<ConflictResponse> conflicts) {

    public enum BlockType {
        /** Getting ready, before leaving. */
        PREP,
        /** On the way to the place. */
        TRAVEL,
        SCHEDULE
    }

    public static TimelineResponse from(LocalDate date, List<Occurrence> occurrences,
            Map<ChecklistLinks.Key, ChecklistSummary> checklists) {
        List<Occurrence> sorted = occurrences.stream().sorted(Comparator.comparing(Occurrence::startAt)).toList();
        List<Block> blocks = new ArrayList<>();
        for (Occurrence occurrence : sorted) {
            Long id = occurrence.schedule().getId();
            String title = occurrence.schedule().getTitle();
            if (occurrence.prepStartAt().isBefore(occurrence.departAt())) {
                blocks.add(new Block(BlockType.PREP, id, occurrence.prepStartAt(), occurrence.departAt(), title + " 준비"));
            }
            if (occurrence.departAt().isBefore(occurrence.startAt())) {
                blocks.add(new Block(BlockType.TRAVEL, id, occurrence.departAt(), occurrence.startAt(), travelLabel(occurrence)));
            }
            blocks.add(new Block(BlockType.SCHEDULE, id, occurrence.startAt(), occurrence.endAt(), title));
        }
        blocks.sort(Comparator.comparing(Block::start));
        return new TimelineResponse(
                date,
                sorted.stream().map(o -> TimelineSchedule.from(o, checklists.get(ChecklistLinks.Key.of(o)))).toList(),
                blocks,
                ScheduleConflict.find(sorted).stream().map(ConflictResponse::from).toList());
    }

    /** "버스 · 강남(으)로 이동". */
    private static String travelLabel(Occurrence occurrence) {
        var schedule = occurrence.schedule();
        String mode = schedule.getTravelMode() == null ? "" : schedule.getTravelMode().getLabel() + " · ";
        String to = schedule.getPlace() == null ? "" : schedule.getPlace() + "(으)로 ";
        return mode + to + "이동";
    }

    /**
     * @param end {@code null} for a schedule without an end time
     */
    public record Block(BlockType type, Long scheduleId, LocalDateTime start, LocalDateTime end, String label) {
    }

    /**
     * @param prepStartAt when to start getting ready
     * @param departAt    when to leave
     * @param checklist   the feature 2 checklist opened for this schedule, or {@code null}
     */
    public record TimelineSchedule(
            Long scheduleId,
            String title,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String place,
            String memo,
            RepeatType repeatType,
            Integer prepMinutes,
            Integer travelMinutes,
            TravelMode travelMode,
            LocalDateTime prepStartAt,
            LocalDateTime departAt,
            ChecklistSummary checklist) {

        static TimelineSchedule from(Occurrence occurrence, ChecklistSummary checklist) {
            var schedule = occurrence.schedule();
            return new TimelineSchedule(schedule.getId(), schedule.getTitle(), occurrence.startAt(), occurrence.endAt(),
                    schedule.getPlace(), schedule.getMemo(), schedule.getRepeatType(), schedule.getPrepMinutes(),
                    schedule.getTravelMinutes(), schedule.getTravelMode(), occurrence.prepStartAt(), occurrence.departAt(),
                    checklist);
        }
    }
}
