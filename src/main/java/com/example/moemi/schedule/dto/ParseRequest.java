package com.example.moemi.schedule.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param text      e.g. "내일 오후 2시에 강남에서 미팅"
 * @param originLat where the user is now, to suggest a travel time; optional
 */
public record ParseRequest(
        @NotBlank(message = "일정 내용을 입력해 주세요.")
        @Size(max = 200, message = "일정 내용은 200자 이하여야 합니다.") String text,
        @DecimalMin(value = "-90", message = "originLat는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90", message = "originLat는 90 이하여야 합니다.") Double originLat,
        @DecimalMin(value = "-180", message = "originLng는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180", message = "originLng는 180 이하여야 합니다.") Double originLng) {
}
