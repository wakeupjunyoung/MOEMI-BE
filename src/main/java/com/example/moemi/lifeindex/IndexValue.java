package com.example.moemi.lifeindex;

import java.time.LocalDateTime;

/**
 * One index for one day: the day's worst (maximum) forecast value and the announcement it came from.
 */
public record IndexValue(LifeIndexType type, double value, IndexGrade grade, LocalDateTime issuedAt) {

    public static IndexValue of(LifeIndexType type, double value, LocalDateTime issuedAt) {
        return new IndexValue(type, value, type.grade(value), issuedAt);
    }
}
