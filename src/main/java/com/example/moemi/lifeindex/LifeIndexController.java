package com.example.moemi.lifeindex;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.moemi.lifeindex.dto.LifeIndexCalendarResponse;
import com.example.moemi.lifeindex.dto.RegionResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/life-index")
@RequiredArgsConstructor
public class LifeIndexController {

    /** Default range when none is given: today and the following days KMA usually forecasts. */
    private static final int DEFAULT_DAYS = 4;

    private final LifeIndexService lifeIndexService;
    private final Clock clock;

    @GetMapping("/regions")
    public List<RegionResponse> regions() {
        return Arrays.stream(Region.values()).map(RegionResponse::from).toList();
    }

    /**
     * Indices per date for the calendar, e.g. {@code GET /api/life-index?regionCode=1233000000&from=2026-10-01&to=2026-10-31}.
     */
    @GetMapping
    public LifeIndexCalendarResponse calendar(
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "false") boolean refresh) {
        Region region = Region.require(regionCode);
        LocalDate start = from != null ? from : LocalDate.now(clock);
        LocalDate end = to != null ? to : start.plusDays(DEFAULT_DAYS - 1);
        return LifeIndexCalendarResponse.from(lifeIndexService.getCalendar(region, start, end, refresh));
    }
}
