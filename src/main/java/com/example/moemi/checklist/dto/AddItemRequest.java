package com.example.moemi.checklist.dto;

import com.example.moemi.checklist.ChecklistItem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddItemRequest(
        @NotBlank(message = "준비물 이름을 입력해 주세요.")
        @Size(max = ChecklistItem.NAME_MAX_LENGTH, message = "준비물 이름은 50자 이하여야 합니다.")
        String name) {
}
