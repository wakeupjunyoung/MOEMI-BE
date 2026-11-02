package com.example.moemi.transit.favorite;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.moemi.common.web.DeviceId;
import com.example.moemi.transit.favorite.dto.AddFavoriteRequest;
import com.example.moemi.transit.favorite.dto.FavoriteRealtimeResponse;
import com.example.moemi.transit.favorite.dto.FavoriteResponse;
import com.example.moemi.transit.favorite.dto.ReorderFavoritesRequest;
import com.example.moemi.transit.favorite.dto.UpdateFavoriteRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Every endpoint requires the {@value DeviceId#HEADER} header and only sees that device's favorites.
 */
@RestController
@RequestMapping("/api/transit/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /** The favorites alone, in the user's order. Works even when TAGO is down. */
    @GetMapping
    public List<FavoriteResponse> list(DeviceId deviceId) {
        return favoriteService.list(deviceId);
    }

    /** The favorites with arrivals, for the home screen. Poll every 30 seconds or so while it is shown. */
    @GetMapping("/realtime")
    public List<FavoriteRealtimeResponse> realtime(DeviceId deviceId) {
        return favoriteService.realtime(deviceId);
    }

    @PostMapping
    public FavoriteResponse add(DeviceId deviceId, @Valid @RequestBody AddFavoriteRequest request) {
        return favoriteService.add(deviceId, request);
    }

    @PatchMapping("/{favoriteId}")
    public FavoriteResponse update(DeviceId deviceId, @PathVariable Long favoriteId,
            @Valid @RequestBody UpdateFavoriteRequest request) {
        return favoriteService.update(deviceId, favoriteId, request);
    }

    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<Void> remove(DeviceId deviceId, @PathVariable Long favoriteId) {
        favoriteService.remove(deviceId, favoriteId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/order")
    public List<FavoriteResponse> reorder(DeviceId deviceId, @Valid @RequestBody ReorderFavoritesRequest request) {
        return favoriteService.reorder(deviceId, request);
    }
}
