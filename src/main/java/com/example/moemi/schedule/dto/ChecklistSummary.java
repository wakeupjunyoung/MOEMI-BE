package com.example.moemi.schedule.dto;

import com.example.moemi.checklist.Checklist;

/**
 * The checklist (feature 2) opened for a schedule, to show "준비물 2개 남음" and link to it.
 */
public record ChecklistSummary(Long checklistId, int totalCount, int remainingCount) {

    public static ChecklistSummary from(Checklist checklist) {
        int total = checklist.getItems().size();
        return new ChecklistSummary(checklist.getId(), total, total - checklist.checkedCount());
    }
}
