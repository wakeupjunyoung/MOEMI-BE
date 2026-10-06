package com.example.moemi.checklist;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.moemi.common.web.DeviceId;
import com.example.moemi.lifeindex.Region;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Feature 2: items to bring for one date, or for one schedule on that date.
 */
@Entity
@Table(name = "checklist", indexes = @Index(name = "idx_checklist_device_date", columnList = "device_id, checklist_date"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Checklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = DeviceId.MAX_LENGTH)
    private String deviceId;

    @Column(name = "checklist_date", nullable = false)
    private LocalDate date;

    /**
     * The schedule (feature 5) this checklist is for, or {@code null} for the whole day.
     * Not a foreign key until schedules exist; a dangling id means "연결된 일정 없음".
     */
    private Long scheduleId;

    @Column(nullable = false, length = 10)
    private String regionCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationStatus recommendationStatus;

    /** Last time recommendations were refreshed from life indices. */
    private LocalDateTime recommendedAt;

    @OneToMany(mappedBy = "checklist", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ChecklistItem> items = new ArrayList<>();

    /** Recommended items the user deleted or renamed. They are not recommended again for this checklist. */
    @ElementCollection
    @CollectionTable(name = "checklist_dismissed_item", joinColumns = @JoinColumn(name = "checklist_id"))
    @Column(name = "name", nullable = false, length = ChecklistItem.NAME_MAX_LENGTH)
    private Set<String> dismissedNames = new HashSet<>();

    public static Checklist create(String deviceId, LocalDate date, Long scheduleId, Region region,
            RecommendationResult recommendations, LocalDateTime now) {
        Checklist checklist = new Checklist();
        checklist.deviceId = deviceId;
        checklist.date = date;
        checklist.scheduleId = scheduleId;
        checklist.regionCode = region.getCode();
        checklist.applyRecommendations(recommendations, now);
        return checklist;
    }

    public void changeRegion(Region region) {
        this.regionCode = region.getCode();
    }

    /**
     * Brings recommended items in line with the latest indices. The user's own items, and recommendations
     * the user dismissed, are left alone. When indices are unavailable nothing is changed.
     */
    public void applyRecommendations(RecommendationResult result, LocalDateTime now) {
        recommendationStatus = result.status();
        if (result.status() != RecommendationStatus.OK) {
            return;
        }
        recommendedAt = now;

        Set<String> recommendedNames = result.items().stream().map(Recommendation::name).collect(Collectors.toSet());
        items.removeIf(item -> item.isRecommended() && !recommendedNames.contains(item.getName()));

        for (Recommendation recommendation : result.items()) {
            if (dismissedNames.contains(recommendation.name())) {
                continue;
            }
            Optional<ChecklistItem> existing = items.stream()
                    .filter(item -> item.getName().equals(recommendation.name()))
                    .findFirst();
            if (existing.isEmpty()) {
                items.add(ChecklistItem.recommended(this, recommendation));
            } else if (existing.get().isRecommended()) {
                existing.get().updateReason(recommendation.reason());
            }
        }
    }

    public ChecklistItem addCustomItem(String name) {
        ChecklistItem item = ChecklistItem.custom(this, name.trim());
        items.add(item);
        return item;
    }

    public Optional<ChecklistItem> findItem(Long itemId) {
        return items.stream().filter(item -> Objects.equals(item.getId(), itemId)).findFirst();
    }

    public void renameItem(ChecklistItem item, String name) {
        String trimmed = name.trim();
        if (trimmed.equals(item.getName())) {
            return;
        }
        if (item.isRecommended()) {
            dismissedNames.add(item.getName());
        }
        item.rename(trimmed);
    }

    public void removeItem(ChecklistItem item) {
        if (item.isRecommended()) {
            dismissedNames.add(item.getName());
        }
        items.remove(item);
    }

    public int checkedCount() {
        return (int) items.stream().filter(ChecklistItem::isChecked).count();
    }
}
