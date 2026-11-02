package com.example.moemi.transit.favorite;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByDeviceIdOrderBySortOrderAscIdAsc(String deviceId);

    Optional<Favorite> findByIdAndDeviceId(Long id, String deviceId);

    long countByDeviceId(String deviceId);

    /** A {@code null} nodeId or routeId matches {@code IS NULL}. */
    boolean existsByDeviceIdAndTypeAndCityCodeAndNodeIdAndRouteId(String deviceId, FavoriteType type, String cityCode,
            String nodeId, String routeId);
}
