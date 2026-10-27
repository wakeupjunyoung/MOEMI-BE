package com.example.moemi.lifeindex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.moemi.common.error.ApiException;
import com.example.moemi.lifeindex.kma.IndexForecast;
import com.example.moemi.lifeindex.kma.IndexForecast.HourlyValue;
import com.example.moemi.lifeindex.kma.KmaLifeIndexClient;
import com.example.moemi.lifeindex.kma.KmaProperties;

class LifeIndexServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 6);
    /** 10:30, so the latest 3-hour announcement is 09:00. */
    private static final LocalDateTime NOW = TODAY.atTime(10, 30);
    private static final LocalDateTime LATEST = TODAY.atTime(9, 0);
    private static final Region REGION = Region.GWANGJU_GWANGSAN;

    private final KmaLifeIndexClient client = mock(KmaLifeIndexClient.class);
    private LifeIndexService service;

    @BeforeEach
    void setUp() {
        KmaProperties properties = new KmaProperties("key", "https://example.invalid", Duration.ofMinutes(30), Duration.ofSeconds(5));
        service = new LifeIndexService(client, properties, Clock.fixed(NOW.atZone(KST).toInstant(), KST));
    }

    @Test
    void marksEachDatePastAvailableOrNoData() {
        givenUv(Optional.of(uvForecast()));
        givenAir(LATEST, Optional.of(airForecast(LATEST)));

        LifeIndexCalendar calendar = service.getCalendar(REGION, TODAY.minusDays(1), TODAY.plusDays(3), false);

        assertThat(calendar.status()).isEqualTo(LifeIndexStatus.OK);
        assertThat(calendar.fetchedAt()).isEqualTo(NOW);
        assertThat(calendar.days()).extracting(LifeIndexCalendar.Day::availability).containsExactly(
                DayAvailability.PAST, DayAvailability.AVAILABLE, DayAvailability.AVAILABLE,
                DayAvailability.NO_DATA, DayAvailability.NO_DATA);

        DailyLifeIndex today = calendar.days().get(1).index();
        assertThat(today.find(LifeIndexType.UV)).get().satisfies(uv -> {
            assertThat(uv.value()).isEqualTo(7.0);
            assertThat(uv.grade()).isEqualTo(new IndexGrade("높음", Level.CAUTION));
            assertThat(uv.issuedAt()).isEqualTo(LATEST);
        });
        assertThat(today.find(LifeIndexType.AIR_STAGNATION)).get()
                .extracting(index -> index.grade().level()).isEqualTo(Level.NORMAL);
        assertThat(today.overallLevel()).isEqualTo(Level.CAUTION);
    }

    @Test
    void fallsBackToPreviousAnnouncementWhenLatestHasNoData() {
        LocalDateTime previous = LATEST.minusHours(3);
        givenUv(Optional.of(uvForecast()));
        givenAir(LATEST, Optional.empty());
        givenAir(previous, Optional.of(airForecast(previous)));

        LifeIndexCalendar calendar = service.getCalendar(REGION, TODAY, TODAY, false);

        assertThat(calendar.days().get(0).index().find(LifeIndexType.AIR_STAGNATION)).get()
                .extracting(IndexValue::issuedAt).isEqualTo(previous);
    }

    @Test
    void reusesCachedIndicesUntilRefreshIsRequested() {
        givenUv(Optional.of(uvForecast()));
        givenAir(LATEST, Optional.of(airForecast(LATEST)));

        service.getCalendar(REGION, TODAY, TODAY, false);
        service.findDay(REGION, TODAY);
        verify(client, times(1)).fetch(eq(LifeIndexType.UV), any(), any());

        service.getCalendar(REGION, TODAY, TODAY, true);
        verify(client, times(2)).fetch(eq(LifeIndexType.UV), any(), any());
    }

    @Test
    void servesLastValuesAsStaleWhenKmaFails() {
        givenUv(Optional.of(uvForecast()));
        givenAir(LATEST, Optional.of(airForecast(LATEST)));
        service.getCalendar(REGION, TODAY, TODAY, false);

        when(client.fetch(eq(LifeIndexType.UV), any(), any())).thenThrow(new LifeIndexFetchException("KMA getUVIdxV5 responded 500"));
        LifeIndexCalendar calendar = service.getCalendar(REGION, TODAY, TODAY, true);

        assertThat(calendar.status()).isEqualTo(LifeIndexStatus.STALE);
        assertThat(calendar.days().get(0).availability()).isEqualTo(DayAvailability.AVAILABLE);
    }

    @Test
    void failsWithoutDaysWhenKmaFailsAndNothingIsCached() {
        when(client.fetch(any(), any(), any())).thenThrow(new LifeIndexFetchException("KMA service key is not configured"));

        LifeIndexCalendar calendar = service.getCalendar(REGION, TODAY, TODAY.plusDays(3), false);

        assertThat(calendar.status()).isEqualTo(LifeIndexStatus.FAILED);
        assertThat(calendar.fetchedAt()).isNull();
        assertThat(calendar.days()).isEmpty();
        assertThat(service.findDay(REGION, TODAY).failed()).isTrue();
    }

    @Test
    void rejectsInvalidRanges() {
        assertThatThrownBy(() -> service.getCalendar(REGION, TODAY, TODAY.minusDays(1), false))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.getCalendar(REGION, TODAY, TODAY.plusDays(LifeIndexService.MAX_RANGE_DAYS), false))
                .isInstanceOf(ApiException.class);
    }

    private void givenUv(Optional<IndexForecast> forecast) {
        when(client.fetch(eq(LifeIndexType.UV), eq(REGION.getCode()), eq(LATEST))).thenReturn(forecast);
    }

    private void givenAir(LocalDateTime announcedAt, Optional<IndexForecast> forecast) {
        when(client.fetch(eq(LifeIndexType.AIR_STAGNATION), eq(REGION.getCode()), eq(announcedAt))).thenReturn(forecast);
    }

    /** UV peaks at 7 today and 4 tomorrow. */
    private static IndexForecast uvForecast() {
        return new IndexForecast(LifeIndexType.UV, LATEST, LATEST.plusHours(45), List.of(
                new HourlyValue(TODAY.atTime(9, 0), 3),
                new HourlyValue(TODAY.atTime(12, 0), 7),
                new HourlyValue(TODAY.plusDays(1).atTime(12, 0), 4)));
    }

    private static IndexForecast airForecast(LocalDateTime issuedAt) {
        return new IndexForecast(LifeIndexType.AIR_STAGNATION, issuedAt, issuedAt.plusHours(48), List.of(
                new HourlyValue(TODAY.atTime(12, 0), 50),
                new HourlyValue(TODAY.plusDays(1).atTime(12, 0), 25)));
    }
}
