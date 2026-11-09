package com.example.moemi.schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    Optional<Schedule> findByIdAndDeviceId(Long id, String deviceId);

    /** Schedules that start on or before {@code date}; the caller filters by repetition. */
    List<Schedule> findByDeviceIdAndDateLessThanEqual(String deviceId, LocalDate date);
}
