package com.example.moemi.lifeindex.dto;

import java.time.LocalDate;
import java.util.List;

import com.example.moemi.lifeindex.DayAvailability;
import com.example.moemi.lifeindex.LifeIndexCalendar;
import com.example.moemi.lifeindex.Level;

/**
 * @param overallLevel the worst level of the day for the calendar cell, or {@code null} without data
 */
public record DayResponse(LocalDate date, DayAvailability availability, Level overallLevel, List<IndexResponse> indices) {

    public static DayResponse from(LifeIndexCalendar.Day day) {
        if (day.index() == null) {
            return new DayResponse(day.date(), day.availability(), null, List.of());
        }
        List<IndexResponse> indices = day.index().indices().stream().map(IndexResponse::from).toList();
        return new DayResponse(day.date(), day.availability(), day.index().overallLevel(), indices);
    }
}
