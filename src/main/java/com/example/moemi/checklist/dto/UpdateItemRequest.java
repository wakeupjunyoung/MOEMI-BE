package com.example.moemi.checklist.dto;

import com.example.moemi.checklist.ChecklistItem;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Partial update: fields left out (or {@code null}) are not changed.
 */
public record UpdateItemRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "준비물 이름을 입력해 주세요.")
        @Size(max = ChecklistItem.NAME_MAX_LENGTH, message = "준비물 이름은 50자 이하여야 합니다.")
        String name,
        Boolean checked) {
}
