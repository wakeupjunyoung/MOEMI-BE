package com.example.moemi.transit.favorite.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * @param favoriteIds every favorite of the device, in the new order
 */
public record ReorderFavoritesRequest(
        @NotNull(message = "favoriteIds를 입력해 주세요.") List<@NotNull(message = "favoriteIds에 빈 값이 있습니다.") Long> favoriteIds) {
}
