package com.example.moemi.schedule.travel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.moemi.hazard.GeoPoint;

class TravelTimeServiceTest {

    private static final String DEVICE = "device";
    private static final GeoPoint HOME = new GeoPoint(35.1650, 126.9090);
    /** About 300 m north: a few minutes' walk. */
    private static final GeoPoint NEAR = new GeoPoint(35.1677, 126.9090);
    /** About 5 km north. */
    private static final GeoPoint FAR = new GeoPoint(35.2100, 126.9090);

    private final OdsayClient odsay = mock(OdsayClient.class);
    private final TravelPreferenceRepository preferences = mock(TravelPreferenceRepository.class);
    private final TravelTimeService service = new TravelTimeService(odsay, preferences);

    @BeforeEach
    void setUp() {
        when(preferences.findByDeviceId(any())).thenReturn(Optional.empty());
    }

    @Test
    void placeWithinTenMinutesWalkIsAlwaysWalked() {
        TravelEstimate estimate = service.estimate(DEVICE, HOME, NEAR, TravelMode.BUS);

        assertThat(estimate.status()).isEqualTo(TravelStatus.OK);
        assertThat(estimate.mode()).isEqualTo(TravelMode.WALK);
        assertThat(estimate.requestedMode()).isEqualTo(TravelMode.BUS);
        assertThat(estimate.forcedWalk()).isTrue();
        assertThat(estimate.minutes()).isBetween(5, 10);
        verifyNoInteractions(odsay);
    }

    @Test
    void defaultModeIsBusAndUserRoundingIsApplied() {
        TravelPreference preference = TravelPreference.create(DEVICE);
        preference.change(TravelMode.BUS, 0, 10);
        when(preferences.findByDeviceId(DEVICE)).thenReturn(Optional.of(preference));
        when(odsay.route(HOME, FAR)).thenReturn(Optional.of(
                new OdsayClient.BusRoute(25, 2, 400, List.of("송정19", "1187"), false)));

        TravelEstimate estimate = service.estimate(DEVICE, HOME, FAR, null);

        assertThat(estimate.mode()).isEqualTo(TravelMode.BUS);
        assertThat(estimate.forcedWalk()).isFalse();
        assertThat(estimate.minutes()).isEqualTo(25);
        assertThat(estimate.suggestedMinutes()).isEqualTo(30);
        assertThat(estimate.summary()).isEqualTo("송정19 → 1187 (환승 1회)");
    }

    @Test
    void bufferIsAddedBeforeRounding() {
        TravelPreference preference = TravelPreference.create(DEVICE);
        preference.change(TravelMode.BUS, 5, 15);
        when(preferences.findByDeviceId(DEVICE)).thenReturn(Optional.of(preference));
        when(odsay.route(HOME, FAR)).thenReturn(Optional.of(new OdsayClient.BusRoute(11, 1, 200, List.of("1187"), false)));

        TravelEstimate estimate = service.estimate(DEVICE, HOME, FAR, null);

        assertThat(estimate.minutes()).isEqualTo(11);
        assertThat(estimate.suggestedMinutes()).isEqualTo(30);
        assertThat(estimate.summary()).isEqualTo("1187");
    }

    @Test
    void walkModeNeverCallsOdsay() {
        TravelEstimate estimate = service.estimate(DEVICE, HOME, FAR, TravelMode.WALK);

        assertThat(estimate.mode()).isEqualTo(TravelMode.WALK);
        assertThat(estimate.forcedWalk()).isFalse();
        verifyNoInteractions(odsay);
    }

    @Test
    void busTooCloseFallsBackToWalking() {
        when(odsay.route(HOME, FAR)).thenReturn(Optional.of(OdsayClient.BusRoute.TOO_CLOSE));

        TravelEstimate estimate = service.estimate(DEVICE, HOME, FAR, TravelMode.BUS);

        assertThat(estimate.mode()).isEqualTo(TravelMode.WALK);
        assertThat(estimate.forcedWalk()).isTrue();
    }

    @Test
    void odsayFailureIsUnavailableButStillGivesWalkingTime() {
        when(odsay.route(any(), any())).thenThrow(new TravelFetchException("ODsay responded 500"));

        TravelEstimate estimate = service.estimate(DEVICE, HOME, FAR, TravelMode.BUS);

        assertThat(estimate.status()).isEqualTo(TravelStatus.UNAVAILABLE);
        assertThat(estimate.minutes()).isNull();
        assertThat(estimate.walkMinutes()).isGreaterThan(TravelTimeService.ALWAYS_WALK_MINUTES);
    }

    @Test
    void missingCoordinatesAreReportedWithoutCallingOdsay() {
        assertThat(service.estimate(DEVICE, HOME, null, null).status()).isEqualTo(TravelStatus.NO_DESTINATION);
        assertThat(service.estimate(DEVICE, null, FAR, null).status()).isEqualTo(TravelStatus.NO_ORIGIN);
        verifyNoInteractions(odsay);
    }

    @Test
    void walkingTimeAllowsForStreetDetours() {
        // 1 km straight: 1.3 km of streets at 4 km/h is about 20 minutes.
        assertThat(TravelTimeService.walkMinutes(1000)).isEqualTo(20);
    }
}
