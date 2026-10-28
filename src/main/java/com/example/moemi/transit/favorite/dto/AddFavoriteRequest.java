package com.example.moemi.transit.favorite.dto;

import com.example.moemi.transit.favorite.Favorite;
import com.example.moemi.transit.favorite.FavoriteType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The names are copied from the search result so the favorite can still be shown when TAGO is down.
 * Stop fields are required for STOP and STOP_ROUTE, route fields for ROUTE and STOP_ROUTE.
 *
 * @param cityCode  omit for the default city
 * @param direction the direction label from the route detail, e.g. "광주역 방면"
 * @param alias     the user's own name for it, e.g. "우리집 앞"
 */
public record AddFavoriteRequest(
        @NotNull(message = "type을 입력해 주세요.") FavoriteType type,
        @Pattern(regexp = "^\\d{1,5}$", message = "cityCode는 숫자여야 합니다.") String cityCode,
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,30}$", message = "nodeId 형식이 올바르지 않습니다.") String nodeId,
        @Size(max = Favorite.NAME_MAX_LENGTH, message = "nodeName은 100자 이하여야 합니다.") String nodeName,
        @Size(max = 20, message = "nodeNo는 20자 이하여야 합니다.") String nodeNo,
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,30}$", message = "routeId 형식이 올바르지 않습니다.") String routeId,
        @Size(max = 30, message = "routeNo는 30자 이하여야 합니다.") String routeNo,
        @Size(max = 30, message = "routeType은 30자 이하여야 합니다.") String routeType,
        @Size(max = Favorite.NAME_MAX_LENGTH, message = "direction은 100자 이하여야 합니다.") String direction,
        @Size(max = Favorite.ALIAS_MAX_LENGTH, message = "별칭은 30자 이하여야 합니다.") String alias) {
}
