package com.example.moemi.lifeindex;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.moemi.common.error.ApiException;
import com.example.moemi.lifeindex.kma.IndexForecast;
import com.example.moemi.lifeindex.kma.KmaLifeIndexClient;
import com.example.moemi.lifeindex.kma.KmaProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Feature 1: life weather indices per date and region, cached per region.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LifeIndexService {

    public static final String SOURCE = "기상청 생활기상지수 조회서비스";

    static final int MAX_RANGE_DAYS = 62;
    private static final int ANNOUNCE_INTERVAL_HOURS = 3;

    private final KmaLifeIndexClient kmaClient;
    private final KmaProperties properties;
    private final Clock clock;

    /** Last successful fetch per region. Kept after it expires so it can be served as STALE. */
    private final Map<Region, Snapshot> cache = new ConcurrentHashMap<>();

    /**
     * @param refresh {@code true} to skip the cache and ask KMA again
     */
    public LifeIndexCalendar getCalendar(Region region, LocalDate from, LocalDate to, boolean refresh) {
        if (to.isBefore(from)) {
            throw ApiException.badRequest("INVALID_RANGE", "to는 from보다 빠를 수 없습니다.");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_RANGE_DAYS) {
            throw ApiException.badRequest("INVALID_RANGE", "한 번에 최대 " + MAX_RANGE_DAYS + "일까지 조회할 수 있습니다.");
        }

        Loaded loaded = load(region, refresh);
        if (loaded.status() == LifeIndexStatus.FAILED) {
            return new LifeIndexCalendar(region, LifeIndexStatus.FAILED, null, List.of());
        }

        LocalDate today = LocalDate.now(clock);
        List<LifeIndexCalendar.Day> days = from.datesUntil(to.plusDays(1))
                .map(date -> toDay(date, today, loaded.snapshot()))
                .toList();
        return new LifeIndexCalendar(region, loaded.status(), loaded.snapshot().fetchedAt(), days);
    }

    /** Indices for one date, served from the cache when possible. */
    public DayLookup findDay(Region region, LocalDate date) {
        Loaded loaded = load(region, false);
        if (loaded.status() == LifeIndexStatus.FAILED) {
            return new DayLookup(LifeIndexStatus.FAILED, null);
        }
        DailyLifeIndex day = date.isBefore(LocalDate.now(clock)) ? null : loaded.snapshot().days().get(date);
        return new DayLookup(loaded.status(), day);
    }

    private static LifeIndexCalendar.Day toDay(LocalDate date, LocalDate today, Snapshot snapshot) {
        if (date.isBefore(today)) {
            return new LifeIndexCalendar.Day(date, DayAvailability.PAST, null);
        }
        DailyLifeIndex index = snapshot.days().get(date);
        if (index == null) {
            return new LifeIndexCalendar.Day(date, DayAvailability.NO_DATA, null);
        }
        return new LifeIndexCalendar.Day(date, DayAvailability.AVAILABLE, index);
    }

    private Loaded load(Region region, boolean refresh) {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        Snapshot cached = cache.get(region);
        if (!refresh && cached != null && cached.fetchedAt().plus(properties.cacheTtl()).isAfter(now)) {
            return new Loaded(LifeIndexStatus.OK, cached);
        }
        try {
            Snapshot fetched = fetch(region, now);
            cache.put(region, fetched);
            return new Loaded(LifeIndexStatus.OK, fetched);
        } catch (LifeIndexFetchException e) {
            log.warn("Life index fetch failed for {}: {}", region, e.getMessage());
            if (cached != null) {
                return new Loaded(LifeIndexStatus.STALE, cached);
            }
            return new Loaded(LifeIndexStatus.FAILED, null);
        }
    }

    private Snapshot fetch(Region region, LocalDateTime now) {
        LocalDateTime latestAnnouncement = now.truncatedTo(ChronoUnit.HOURS)
                .withHour(now.getHour() / ANNOUNCE_INTERVAL_HOURS * ANNOUNCE_INTERVAL_HOURS);

        Map<LocalDate, List<IndexValue>> valuesByDate = new TreeMap<>();
        for (LifeIndexType type : LifeIndexType.values()) {
            Optional<IndexForecast> forecast = kmaClient.fetch(type, region.getCode(), latestAnnouncement);
            if (forecast.isEmpty()) {
                // The latest announcement may not be published yet; fall back to the previous one.
                forecast = kmaClient.fetch(type, region.getCode(), latestAnnouncement.minusHours(ANNOUNCE_INTERVAL_HOURS));
            }
            forecast.ifPresent(f -> f.dailyMax().forEach((date, value) ->
                    valuesByDate.computeIfAbsent(date, d -> new ArrayList<>()).add(IndexValue.of(type, value, f.issuedAt()))));
        }

        Map<LocalDate, DailyLifeIndex> days = new TreeMap<>();
        valuesByDate.forEach((date, values) -> days.put(date, new DailyLifeIndex(date, List.copyOf(values))));
        return new Snapshot(days, now);
    }

    private record Snapshot(Map<LocalDate, DailyLifeIndex> days, LocalDateTime fetchedAt) {
    }

    private record Loaded(LifeIndexStatus status, Snapshot snapshot) {
    }
}
