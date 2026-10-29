package com.example.moemi.transit.favorite;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.moemi.common.error.ApiException;
import com.example.moemi.common.web.DeviceId;
import com.example.moemi.transit.ArrivalLookup;
import com.example.moemi.transit.BusLocationLookup;
import com.example.moemi.transit.RealtimeStatus;
import com.example.moemi.transit.TransitService;
import com.example.moemi.transit.dto.ArrivalResponse;
import com.example.moemi.transit.favorite.dto.AddFavoriteRequest;
import com.example.moemi.transit.favorite.dto.FavoriteRealtimeResponse;
import com.example.moemi.transit.favorite.dto.FavoriteRealtimeResponse.Realtime;
import com.example.moemi.transit.favorite.dto.FavoriteResponse;
import com.example.moemi.transit.favorite.dto.ReorderFavoritesRequest;
import com.example.moemi.transit.favorite.dto.UpdateFavoriteRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Feature 4: favorite stops and routes per device. The list itself never depends on TAGO;
 * real-time state is looked up separately and only ever changes the {@code realtime} part.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    static final int MAX_FAVORITES = 30;
    /** Arrivals shown per favorite on the home screen. */
    static final int ARRIVALS_PER_FAVORITE = 3;

    private final FavoriteRepository favoriteRepository;
    private final TransitService transitService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<FavoriteResponse> list(DeviceId deviceId) {
        return favoriteRepository.findByDeviceIdOrderBySortOrderAscIdAsc(deviceId.value()).stream()
                .map(FavoriteResponse::from)
                .toList();
    }

    /** Adds the favorite at the end of the list. */
    @Transactional
    public FavoriteResponse add(DeviceId deviceId, AddFavoriteRequest request) {
        FavoriteType type = request.type();
        String cityCode = transitService.resolveCityCode(request.cityCode());
        Favorite.StopTarget stop = type.hasStop() ? stopTarget(request) : null;
        Favorite.RouteTarget route = type.hasRoute() ? routeTarget(request) : null;
        String nodeId = stop == null ? null : stop.nodeId();
        String routeId = route == null ? null : route.routeId();

        if (favoriteRepository.existsByDeviceIdAndTypeAndCityCodeAndNodeIdAndRouteId(
                deviceId.value(), type, cityCode, nodeId, routeId)) {
            throw new ApiException(HttpStatus.CONFLICT, "FAVORITE_DUPLICATE", "이미 즐겨찾기에 있습니다.");
        }
        List<Favorite> current = favoriteRepository.findByDeviceIdOrderBySortOrderAscIdAsc(deviceId.value());
        if (current.size() >= MAX_FAVORITES) {
            throw ApiException.badRequest("FAVORITE_LIMIT_EXCEEDED", "즐겨찾기는 최대 " + MAX_FAVORITES + "개까지 등록할 수 있습니다.");
        }
        int nextOrder = current.stream().mapToInt(Favorite::getSortOrder).max().orElse(-1) + 1;

        Favorite favorite = favoriteRepository.save(Favorite.create(
                deviceId.value(), type, cityCode, stop, route, blankToNull(request.alias()), nextOrder, now()));
        return FavoriteResponse.from(favorite);
    }

    @Transactional
    public FavoriteResponse update(DeviceId deviceId, Long favoriteId, UpdateFavoriteRequest request) {
        Favorite favorite = getOwned(deviceId, favoriteId);
        favorite.changeAlias(blankToNull(request.alias()));
        return FavoriteResponse.from(favorite);
    }

    @Transactional
    public void remove(DeviceId deviceId, Long favoriteId) {
        favoriteRepository.delete(getOwned(deviceId, favoriteId));
    }

    /** Puts the favorites in the given order. The ids must be exactly the device's favorites. */
    @Transactional
    public List<FavoriteResponse> reorder(DeviceId deviceId, ReorderFavoritesRequest request) {
        List<Favorite> favorites = favoriteRepository.findByDeviceIdOrderBySortOrderAscIdAsc(deviceId.value());
        Map<Long, Favorite> byId = favorites.stream().collect(Collectors.toMap(Favorite::getId, Function.identity()));
        List<Long> ids = request.favoriteIds();
        if (ids.size() != byId.size() || !new HashSet<>(ids).equals(byId.keySet())) {
            throw ApiException.badRequest("INVALID_FAVORITE_ORDER", "favoriteIds에는 모든 즐겨찾기 id가 한 번씩 있어야 합니다.");
        }
        for (int order = 0; order < ids.size(); order++) {
            byId.get(ids.get(order)).moveTo(order);
        }
        return ids.stream().map(byId::get).map(FavoriteResponse::from).toList();
    }

    /**
     * Every favorite with its real-time state, looked up in parallel so the home screen waits for
     * the slowest one only. A failing lookup marks that favorite FAILED and leaves the others alone.
     */
    public List<FavoriteRealtimeResponse> realtime(DeviceId deviceId) {
        List<Favorite> favorites = favoriteRepository.findByDeviceIdOrderBySortOrderAscIdAsc(deviceId.value());
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Realtime>> lookups = favorites.stream()
                    .map(favorite -> executor.submit(() -> realtimeOf(favorite)))
                    .toList();
            return IntStream.range(0, favorites.size())
                    .mapToObj(i -> new FavoriteRealtimeResponse(FavoriteResponse.from(favorites.get(i)), join(lookups.get(i))))
                    .toList();
        }
    }

    private Realtime realtimeOf(Favorite favorite) {
        String cityCode = favorite.getCityCode();
        if (favorite.getType().hasRoute()
                && transitService.routeExists(cityCode, favorite.getRouteId()).equals(Optional.of(false))) {
            return Realtime.without(RealtimeStatus.ROUTE_NOT_FOUND);
        }
        if (favorite.getType() == FavoriteType.ROUTE) {
            BusLocationLookup lookup = transitService.busLocations(cityCode, favorite.getRouteId());
            Integer running = lookup.fetchedAt() == null ? null : lookup.buses().size();
            return new Realtime(lookup.status(), lookup.fetchedAt(), List.of(), running);
        }
        ArrivalLookup lookup = transitService.arrivals(cityCode, favorite.getNodeId(), favorite.getRouteId());
        List<ArrivalResponse> arrivals = lookup.arrivals().stream()
                .limit(ARRIVALS_PER_FAVORITE)
                .map(arrival -> ArrivalResponse.from(arrival, lookup.fetchedAt()))
                .toList();
        return new Realtime(lookup.status(), lookup.fetchedAt(), arrivals, null);
    }

    private static Realtime join(Future<Realtime> lookup) {
        try {
            return lookup.get();
        } catch (ExecutionException e) {
            log.warn("Favorite real-time lookup failed", e.getCause());
            return Realtime.without(RealtimeStatus.FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Realtime.without(RealtimeStatus.FAILED);
        }
    }

    private static Favorite.StopTarget stopTarget(AddFavoriteRequest request) {
        String nodeId = blankToNull(request.nodeId());
        String nodeName = blankToNull(request.nodeName());
        if (nodeId == null || nodeName == null) {
            throw ApiException.badRequest("INVALID_REQUEST", request.type() + " 즐겨찾기에는 nodeId와 nodeName이 필요합니다.");
        }
        return new Favorite.StopTarget(nodeId, nodeName, blankToNull(request.nodeNo()));
    }

    private static Favorite.RouteTarget routeTarget(AddFavoriteRequest request) {
        String routeId = blankToNull(request.routeId());
        String routeNo = blankToNull(request.routeNo());
        if (routeId == null || routeNo == null) {
            throw ApiException.badRequest("INVALID_REQUEST", request.type() + " 즐겨찾기에는 routeId와 routeNo가 필요합니다.");
        }
        return new Favorite.RouteTarget(routeId, routeNo, blankToNull(request.routeType()), blankToNull(request.direction()));
    }

    /** Another device's favorite is reported as missing, not forbidden, so ids cannot be probed. */
    private Favorite getOwned(DeviceId deviceId, Long favoriteId) {
        return favoriteRepository.findByIdAndDeviceId(favoriteId, deviceId.value())
                .orElseThrow(() -> ApiException.notFound("FAVORITE_NOT_FOUND", "즐겨찾기를 찾을 수 없습니다."));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
