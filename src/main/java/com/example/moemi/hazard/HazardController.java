package com.example.moemi.hazard;

import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.moemi.hazard.dto.HazardSearchResponse;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/hazards")
@RequiredArgsConstructor
public class HazardController {

    private final HazardService hazardService;

    /**
     * Hazards around a point, e.g. {@code GET /api/hazards?lat=35.19&lng=126.82&radius=1000&type=CONSTRUCTION}.
     * For a searched address, the app resolves it to coordinates first.
     *
     * @param type repeat or comma-separate to filter by several types; omit for all
     */
    @GetMapping
    public HazardSearchResponse search(
            @RequestParam @DecimalMin(value = "-90", message = "lat는 -90 이상이어야 합니다.")
            @DecimalMax(value = "90", message = "lat는 90 이하여야 합니다.") double lat,
            @RequestParam @DecimalMin(value = "-180", message = "lng는 -180 이상이어야 합니다.")
            @DecimalMax(value = "180", message = "lng는 180 이하여야 합니다.") double lng,
            @RequestParam(defaultValue = "1000") @Min(value = 100, message = "radius는 100m 이상이어야 합니다.")
            @Max(value = 5000, message = "radius는 5000m 이하여야 합니다.") int radius,
            @RequestParam(required = false) Set<HazardType> type) {
        Set<HazardType> types = type == null ? Set.of() : type;
        return HazardSearchResponse.from(hazardService.search(new GeoPoint(lat, lng), radius, types));
    }
}
