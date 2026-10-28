package com.example.moemi.transit.favorite.dto;

import java.time.LocalDateTime;

import com.example.moemi.transit.favorite.Favorite;
import com.example.moemi.transit.favorite.FavoriteType;

public record FavoriteResponse(
        Long id,
        FavoriteType type,
        String cityCode,
        String nodeId,
        String nodeName,
        String nodeNo,
        String routeId,
        String routeNo,
        String routeType,
        String direction,
        String alias,
        int sortOrder,
        LocalDateTime createdAt) {

    public static FavoriteResponse from(Favorite favorite) {
        return new FavoriteResponse(
                favorite.getId(),
                favorite.getType(),
                favorite.getCityCode(),
                favorite.getNodeId(),
                favorite.getNodeName(),
                favorite.getNodeNo(),
                favorite.getRouteId(),
                favorite.getRouteNo(),
                favorite.getRouteType(),
                favorite.getDirection(),
                favorite.getAlias(),
                favorite.getSortOrder(),
                favorite.getCreatedAt());
    }
}
