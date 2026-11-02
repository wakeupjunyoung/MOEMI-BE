package com.example.moemi.schedule;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.moemi.checklist.Checklist;
import com.example.moemi.checklist.ChecklistRepository;
import com.example.moemi.schedule.dto.ChecklistSummary;

import lombok.RequiredArgsConstructor;

/**
 * Finds the feature 2 checklist opened for each schedule occurrence (checklists store a schedule id).
 */
@Component
@RequiredArgsConstructor
public class ChecklistLinks {

    private final ChecklistRepository checklistRepository;

    /** Checklists of the device on the given dates, by date and schedule id. */
    @Transactional(readOnly = true)
    public Map<Key, ChecklistSummary> find(String deviceId, Collection<LocalDate> dates) {
        Map<Key, ChecklistSummary> links = new HashMap<>();
        for (LocalDate date : dates) {
            for (Checklist checklist : checklistRepository.findByDeviceIdAndDateOrderByIdAsc(deviceId, date)) {
                if (checklist.getScheduleId() != null) {
                    links.putIfAbsent(new Key(date, checklist.getScheduleId()), ChecklistSummary.from(checklist));
                }
            }
        }
        return links;
    }

    public record Key(LocalDate date, Long scheduleId) {

        public static Key of(Occurrence occurrence) {
            return new Key(occurrence.date(), occurrence.schedule().getId());
        }
    }
}
