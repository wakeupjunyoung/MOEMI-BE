package com.example.moemi.checklist;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChecklistRepository extends JpaRepository<Checklist, Long> {

    List<Checklist> findByDeviceIdAndDateOrderByIdAsc(String deviceId, LocalDate date);

    Optional<Checklist> findByIdAndDeviceId(Long id, String deviceId);

    /** A {@code null} scheduleId matches the whole-day checklist ({@code IS NULL}). */
    Optional<Checklist> findByDeviceIdAndDateAndScheduleId(String deviceId, LocalDate date, Long scheduleId);
}
