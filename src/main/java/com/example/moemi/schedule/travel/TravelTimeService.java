package com.example.moemi.schedule.travel;

import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.moemi.common.error.ApiException;
import com.example.moemi.common.web.DeviceId;
import com.example.moemi.hazard.GeoPoint;
import com.example.moemi.schedule.travel.dto.TravelSettingsRequest;
import com.example.moemi.schedule.travel.dto.TravelSettingsResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Suggests how long it takes to reach a schedule's place on foot or by bus, so the app can show
 * when to leave and when to start getting ready. A place within {@value #ALWAYS_WALK_MINUTES}
 * minutes' walk is always walked. Failures are reported as a status and never block saving the schedule.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TravelTimeService {

    /** At or below this many walking minutes, walking is suggested even when the bus was asked for. */
    public static final int ALWAYS_WALK_MINUTES = 10;
    /** 4 km/h. */
    static final double WALK_METERS_PER_MINUTE = 4000.0 / 60;
    /** Streets are longer than the straight line between two points. */
    static final double WALK_DETOUR = 1.3;
    static final int MAX_BUFFER_MINUTES = 60;
    static final Set<Integer> ROUND_UP_CHOICES = Set.of(0, 5, 10, 15, 30);

    private final OdsayClient odsayClient;
    private final TravelPreferenceRepository preferenceRepository;

    /**
     * @param origin        where the user leaves from, usually the phone's location
     * @param destination   the place's coordinates, picked or geocoded in the app
     * @param requestedMode {@code null} for the user's default mode
     */
    @Transactional(readOnly = true)
    public TravelEstimate estimate(String deviceId, GeoPoint origin, GeoPoint destination, TravelMode requestedMode) {
        TravelPreference preference = preference(deviceId);
        TravelMode wanted = requestedMode != null ? requestedMode : preference.getDefaultMode();
        if (destination == null) {
            return TravelEstimate.without(TravelStatus.NO_DESTINATION, wanted);
        }
        if (origin == null) {
            return TravelEstimate.without(TravelStatus.NO_ORIGIN, wanted);
        }

        int distanceM = (int) Math.round(origin.distanceTo(destination));
        int walkMinutes = walkMinutes(distanceM);
        if (walkMinutes <= ALWAYS_WALK_MINUTES || wanted == TravelMode.WALK) {
            return walk(wanted, walkMinutes, distanceM, preference);
        }

        try {
            Optional<OdsayClient.BusRoute> route = odsayClient.route(origin, destination);
            if (route.isPresent() && route.get().tooClose()) {
                return walk(wanted, walkMinutes, distanceM, preference);
            }
            if (route.isPresent()) {
                return ok(TravelMode.BUS, wanted, route.get().totalMinutes(), walkMinutes, distanceM,
                        route.get().summary(), preference);
            }
        } catch (TravelFetchException e) {
            log.warn("Bus travel estimate failed: {}", e.getMessage());
        }
        return new TravelEstimate(TravelStatus.UNAVAILABLE, wanted, wanted, false, null, null, walkMinutes, distanceM, null);
    }

    @Transactional(readOnly = true)
    public TravelSettingsResponse settings(DeviceId deviceId) {
        return TravelSettingsResponse.from(preference(deviceId.value()));
    }

    @Transactional
    public TravelSettingsResponse updateSettings(DeviceId deviceId, TravelSettingsRequest request) {
        int buffer = request.bufferMinutes() == null ? 0 : request.bufferMinutes();
        int roundUp = request.roundUpMinutes() == null ? 0 : request.roundUpMinutes();
        if (buffer < 0 || buffer > MAX_BUFFER_MINUTES) {
            throw ApiException.badRequest("INVALID_TRAVEL_SETTING", "여유 시간은 0분에서 " + MAX_BUFFER_MINUTES + "분 사이여야 합니다.");
        }
        if (!ROUND_UP_CHOICES.contains(roundUp)) {
            throw ApiException.badRequest("INVALID_TRAVEL_SETTING", "반올림 단위는 0, 5, 10, 15, 30분 중 하나여야 합니다.");
        }
        TravelPreference preference = preferenceRepository.findByDeviceId(deviceId.value())
                .orElseGet(() -> preferenceRepository.save(TravelPreference.create(deviceId.value())));
        preference.change(request.defaultMode(), buffer, roundUp);
        return TravelSettingsResponse.from(preference);
    }

    static int walkMinutes(int distanceM) {
        return (int) Math.ceil(distanceM * WALK_DETOUR / WALK_METERS_PER_MINUTE);
    }

    private TravelPreference preference(String deviceId) {
        if (deviceId == null) {
            return TravelPreference.defaults();
        }
        return preferenceRepository.findByDeviceId(deviceId).orElseGet(TravelPreference::defaults);
    }

    private static TravelEstimate walk(TravelMode wanted, int walkMinutes, int distanceM, TravelPreference preference) {
        return ok(TravelMode.WALK, wanted, walkMinutes, walkMinutes, distanceM, TravelMode.WALK.getLabel(), preference);
    }

    private static TravelEstimate ok(TravelMode mode, TravelMode wanted, int minutes, int walkMinutes, int distanceM,
            String summary, TravelPreference preference) {
        return new TravelEstimate(TravelStatus.OK, mode, wanted, mode != wanted, minutes, preference.adjust(minutes),
                walkMinutes, distanceM, summary);
    }
}
