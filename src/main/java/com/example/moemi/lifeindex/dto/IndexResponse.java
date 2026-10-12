package com.example.moemi.lifeindex.dto;

import java.time.LocalDateTime;

import com.example.moemi.lifeindex.IndexValue;
import com.example.moemi.lifeindex.LifeIndexType;
import com.example.moemi.lifeindex.Level;

/**
 * @param value    the day's worst forecast value; whole numbers are sent without a fraction
 * @param unit     unit of {@code value}, or {@code null} when dimensionless
 * @param grade    KMA's grade name, e.g. "높음"
 * @param issuedAt the KMA announcement time the value comes from (발표 기준 시각)
 */
public record IndexResponse(
        LifeIndexType type,
        String name,
        Number value,
        String unit,
        String grade,
        Level level,
        LocalDateTime issuedAt) {

    public static IndexResponse from(IndexValue index) {
        double value = index.value();
        Number number = value == Math.rint(value) ? (Number) (long) value : (Number) value;
        return new IndexResponse(
                index.type(),
                index.type().getDisplayName(),
                number,
                index.type().getUnit(),
                index.grade().label(),
                index.grade().level(),
                index.issuedAt());
    }
}
