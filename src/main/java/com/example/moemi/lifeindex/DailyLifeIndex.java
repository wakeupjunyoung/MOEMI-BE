package com.example.moemi.lifeindex;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * All indices forecast for one date in one region.
 */
public record DailyLifeIndex(LocalDate date, List<IndexValue> indices) {

    /** The worst level of the day, shown as the calendar cell's summary dot. */
    public Level overallLevel() {
        return indices.stream()
                .map(index -> index.grade().level())
                .max(Comparator.naturalOrder())
                .orElse(Level.GOOD);
    }

    public Optional<IndexValue> find(LifeIndexType type) {
        return indices.stream().filter(index -> index.type() == type).findFirst();
    }
}
