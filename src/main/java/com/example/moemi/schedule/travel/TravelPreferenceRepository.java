package com.example.moemi.schedule.travel;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelPreferenceRepository extends JpaRepository<TravelPreference, Long> {

    Optional<TravelPreference> findByDeviceId(String deviceId);
}
