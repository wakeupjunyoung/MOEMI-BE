package com.example.moemi.transit.favorite;

import java.time.LocalDateTime;

import com.example.moemi.common.web.DeviceId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Feature 4: a stop, route, or route at a stop the user wants to see first.
 * Names are stored with the TAGO ids so the list works even when TAGO cannot be reached.
 */
@Entity
@Table(name = "transit_favorite", indexes = @Index(name = "idx_transit_favorite_device", columnList = "device_id, sort_order"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite {

    public static final int NAME_MAX_LENGTH = 100;
    public static final int ALIAS_MAX_LENGTH = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = DeviceId.MAX_LENGTH)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FavoriteType type;

    @Column(nullable = false, length = 10)
    private String cityCode;

    @Column(length = 30)
    private String nodeId;

    @Column(length = NAME_MAX_LENGTH)
    private String nodeName;

    @Column(length = 20)
    private String nodeNo;

    @Column(length = 30)
    private String routeId;

    @Column(length = 30)
    private String routeNo;

    @Column(length = 30)
    private String routeType;

    @Column(length = NAME_MAX_LENGTH)
    private String direction;

    @Column(length = ALIAS_MAX_LENGTH)
    private String alias;

    /** Position in the user's list, from 0. */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * @param stop  stop fields, or {@code null} for a ROUTE favorite
     * @param route route fields, or {@code null} for a STOP favorite
     */
    public static Favorite create(String deviceId, FavoriteType type, String cityCode, StopTarget stop,
            RouteTarget route, String alias, int sortOrder, LocalDateTime now) {
        Favorite favorite = new Favorite();
        favorite.deviceId = deviceId;
        favorite.type = type;
        favorite.cityCode = cityCode;
        if (stop != null) {
            favorite.nodeId = stop.nodeId();
            favorite.nodeName = stop.name();
            favorite.nodeNo = stop.number();
        }
        if (route != null) {
            favorite.routeId = route.routeId();
            favorite.routeNo = route.routeNo();
            favorite.routeType = route.routeType();
            favorite.direction = route.direction();
        }
        favorite.alias = alias;
        favorite.sortOrder = sortOrder;
        favorite.createdAt = now;
        return favorite;
    }

    public void changeAlias(String alias) {
        this.alias = alias;
    }

    public void moveTo(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public record StopTarget(String nodeId, String name, String number) {
    }

    public record RouteTarget(String routeId, String routeNo, String routeType, String direction) {
    }
}
