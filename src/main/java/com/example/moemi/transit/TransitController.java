package com.example.moemi.transit;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.moemi.hazard.GeoPoint;
import com.example.moemi.transit.dto.ArrivalLookupResponse;
import com.example.moemi.transit.dto.BusLocationResponse;
import com.example.moemi.transit.dto.NearbyStopsResponse;
import com.example.moemi.transit.dto.RouteDetailResponse;
import com.example.moemi.transit.dto.RouteSearchResponse;
import com.example.moemi.transit.dto.StopSearchResponse;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/**
 * Search and lookup of bus stops and routes. {@code cityCode} is optional everywhere and defaults to
 * the first supported city ({@code moemi.tago.default-city-code}).
 * TAGO failures are reported in the {@code status} field with 200, not as HTTP errors.
 */
@RestController
@RequestMapping("/api/transit")
@RequiredArgsConstructor
public class TransitController {

    static final String CITY_CODE = "^\\d{1,5}$";
    static final String TAGO_ID = "^[A-Za-z0-9_-]{1,30}$";

    private final TransitService transitService;

    @GetMapping("/cities")
    public List<City> cities() {
        return transitService.cities();
    }

    /** Stops by name or number, e.g. {@code GET /api/transit/stops?keyword=광주역}. */
    @GetMapping("/stops")
    public StopSearchResponse searchStops(
            @RequestParam(required = false) @Pattern(regexp = CITY_CODE, message = "cityCode는 숫자여야 합니다.") String cityCode,
            @RequestParam @NotBlank(message = "검색어를 입력해 주세요.")
            @Size(max = 30, message = "검색어는 30자 이하여야 합니다.") String keyword) {
        return StopSearchResponse.from(transitService.searchStops(transitService.resolveCityCode(cityCode), keyword));
    }

    /** Stops within about 500 m, nearest first, e.g. {@code GET /api/transit/stops/nearby?lat=35.165&lng=126.909}. */
    @GetMapping("/stops/nearby")
    public NearbyStopsResponse nearbyStops(
            @RequestParam @DecimalMin(value = "-90", message = "lat는 -90 이상이어야 합니다.")
            @DecimalMax(value = "90", message = "lat는 90 이하여야 합니다.") double lat,
            @RequestParam @DecimalMin(value = "-180", message = "lng는 -180 이상이어야 합니다.")
            @DecimalMax(value = "180", message = "lng는 180 이하여야 합니다.") double lng) {
        return NearbyStopsResponse.from(transitService.nearbyStops(new GeoPoint(lat, lng)));
    }

    /** Arrivals at a stop, optionally of one route, e.g. {@code GET /api/transit/stops/GJB5000001/arrivals?routeId=GJB300}. */
    @GetMapping("/stops/{nodeId}/arrivals")
    public ArrivalLookupResponse arrivals(
            @PathVariable @Pattern(regexp = TAGO_ID, message = "nodeId 형식이 올바르지 않습니다.") String nodeId,
            @RequestParam(required = false) @Pattern(regexp = CITY_CODE, message = "cityCode는 숫자여야 합니다.") String cityCode,
            @RequestParam(required = false) @Pattern(regexp = TAGO_ID, message = "routeId 형식이 올바르지 않습니다.") String routeId) {
        String city = transitService.resolveCityCode(cityCode);
        return ArrivalLookupResponse.from(city, nodeId, routeId, transitService.arrivals(city, nodeId, routeId));
    }

    @GetMapping("/stops/{nodeId}/routes")
    public RouteSearchResponse routesOfStop(
            @PathVariable @Pattern(regexp = TAGO_ID, message = "nodeId 형식이 올바르지 않습니다.") String nodeId,
            @RequestParam(required = false) @Pattern(regexp = CITY_CODE, message = "cityCode는 숫자여야 합니다.") String cityCode) {
        return RouteSearchResponse.from(transitService.routesOfStop(transitService.resolveCityCode(cityCode), nodeId));
    }

    /** Routes by number, e.g. {@code GET /api/transit/routes?keyword=302}. */
    @GetMapping("/routes")
    public RouteSearchResponse searchRoutes(
            @RequestParam(required = false) @Pattern(regexp = CITY_CODE, message = "cityCode는 숫자여야 합니다.") String cityCode,
            @RequestParam @NotBlank(message = "검색어를 입력해 주세요.")
            @Size(max = 30, message = "검색어는 30자 이하여야 합니다.") String keyword) {
        return RouteSearchResponse.from(transitService.searchRoutes(transitService.resolveCityCode(cityCode), keyword));
    }

    /** A route and its stops split by direction. */
    @GetMapping("/routes/{routeId}")
    public RouteDetailResponse routeDetail(
            @PathVariable @Pattern(regexp = TAGO_ID, message = "routeId 형식이 올바르지 않습니다.") String routeId,
            @RequestParam(required = false) @Pattern(regexp = CITY_CODE, message = "cityCode는 숫자여야 합니다.") String cityCode) {
        return RouteDetailResponse.from(transitService.routeDetail(transitService.resolveCityCode(cityCode), routeId));
    }

    @GetMapping("/routes/{routeId}/locations")
    public BusLocationResponse busLocations(
            @PathVariable @Pattern(regexp = TAGO_ID, message = "routeId 형식이 올바르지 않습니다.") String routeId,
            @RequestParam(required = false) @Pattern(regexp = CITY_CODE, message = "cityCode는 숫자여야 합니다.") String cityCode) {
        String city = transitService.resolveCityCode(cityCode);
        return BusLocationResponse.from(city, routeId, transitService.busLocations(city, routeId));
    }
}
