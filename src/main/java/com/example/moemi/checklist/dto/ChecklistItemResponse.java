package com.example.moemi.checklist.dto;

import com.example.moemi.checklist.ChecklistItem;
import com.example.moemi.checklist.ItemSource;

/**
 * @param reason why the app recommended the item, or {@code null} for the user's own items
 */
public record ChecklistItemResponse(Long id, String name, boolean checked, ItemSource source, String reason) {

    public static ChecklistItemResponse from(ChecklistItem item) {
        return new ChecklistItemResponse(item.getId(), item.getName(), item.isChecked(), item.getSource(), item.getReason());
    }
}
