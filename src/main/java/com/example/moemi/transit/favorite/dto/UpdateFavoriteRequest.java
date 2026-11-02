package com.example.moemi.transit.favorite.dto;

import com.example.moemi.transit.favorite.Favorite;

import jakarta.validation.constraints.Size;

/**
 * @param alias the new alias; blank or {@code null} removes it
 */
public record UpdateFavoriteRequest(
        @Size(max = Favorite.ALIAS_MAX_LENGTH, message = "별칭은 30자 이하여야 합니다.") String alias) {
}
