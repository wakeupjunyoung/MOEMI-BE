package com.example.moemi.checklist;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.moemi.checklist.dto.AddItemRequest;
import com.example.moemi.checklist.dto.ChecklistResponse;
import com.example.moemi.checklist.dto.OpenChecklistRequest;
import com.example.moemi.checklist.dto.UpdateItemRequest;
import com.example.moemi.common.error.ApiException;
import com.example.moemi.common.web.DeviceId;
import com.example.moemi.lifeindex.DayLookup;
import com.example.moemi.lifeindex.LifeIndexService;
import com.example.moemi.lifeindex.Region;

/**
 * Feature 2: pre-departure checklists. Methods that need life indices look them up before opening
 * a transaction, so a slow KMA call never holds a database connection.
 */
@Service
public class ChecklistService {

    private final ChecklistRepository checklistRepository;
    private final LifeIndexService lifeIndexService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public ChecklistService(ChecklistRepository checklistRepository, LifeIndexService lifeIndexService,
            PlatformTransactionManager transactionManager, Clock clock) {
        this.checklistRepository = checklistRepository;
        this.lifeIndexService = lifeIndexService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    /** Returns the checklist for the date (and schedule), creating it with recommendations the first time. */
    public ChecklistResponse open(DeviceId deviceId, OpenChecklistRequest request) {
        Region region = Region.require(request.regionCode());
        RecommendationResult recommendations = recommend(region, request.date());
        return transactionTemplate.execute(status -> {
            Checklist checklist = checklistRepository
                    .findByDeviceIdAndDateAndScheduleId(deviceId.value(), request.date(), request.scheduleId())
                    .orElseGet(() -> checklistRepository.save(Checklist.create(
                            deviceId.value(), request.date(), request.scheduleId(), region, recommendations, now())));
            checklistRepository.flush();
            return ChecklistResponse.from(checklist);
        });
    }

    /** All checklists of a date, for the per-schedule and combined views. */
    @Transactional(readOnly = true)
    public List<ChecklistResponse> findByDate(DeviceId deviceId, LocalDate date) {
        return checklistRepository.findByDeviceIdAndDateOrderByIdAsc(deviceId.value(), date).stream()
                .map(ChecklistResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChecklistResponse get(DeviceId deviceId, Long checklistId) {
        return ChecklistResponse.from(getOwned(deviceId, checklistId));
    }

    /**
     * Re-applies recommendations from the latest indices.
     *
     * @param regionCode a new region to use from now on, or {@code null} to keep the checklist's region
     */
    public ChecklistResponse refreshRecommendations(DeviceId deviceId, Long checklistId, String regionCode) {
        Checklist current = getOwned(deviceId, checklistId);
        Region region = Region.require(regionCode != null ? regionCode : current.getRegionCode());
        RecommendationResult recommendations = recommend(region, current.getDate());
        return transactionTemplate.execute(status -> {
            Checklist checklist = getOwned(deviceId, checklistId);
            checklist.changeRegion(region);
            checklist.applyRecommendations(recommendations, now());
            checklistRepository.flush();
            return ChecklistResponse.from(checklist);
        });
    }

    @Transactional
    public ChecklistResponse addItem(DeviceId deviceId, Long checklistId, AddItemRequest request) {
        Checklist checklist = getOwned(deviceId, checklistId);
        checklist.addCustomItem(request.name());
        checklistRepository.flush();
        return ChecklistResponse.from(checklist);
    }

    @Transactional
    public ChecklistResponse updateItem(DeviceId deviceId, Long checklistId, Long itemId, UpdateItemRequest request) {
        Checklist checklist = getOwned(deviceId, checklistId);
        ChecklistItem item = getItem(checklist, itemId);
        if (request.name() != null) {
            checklist.renameItem(item, request.name());
        }
        if (request.checked() != null) {
            item.check(request.checked());
        }
        return ChecklistResponse.from(checklist);
    }

    @Transactional
    public ChecklistResponse removeItem(DeviceId deviceId, Long checklistId, Long itemId) {
        Checklist checklist = getOwned(deviceId, checklistId);
        checklist.removeItem(getItem(checklist, itemId));
        return ChecklistResponse.from(checklist);
    }

    private RecommendationResult recommend(Region region, LocalDate date) {
        DayLookup lookup = lifeIndexService.findDay(region, date);
        if (lookup.failed()) {
            return RecommendationResult.failed();
        }
        if (lookup.day() == null) {
            return RecommendationResult.noData();
        }
        return RecommendationResult.ok(PackingRecommender.recommend(lookup.day()));
    }

    /** Another device's checklist is reported as missing, not forbidden, so ids cannot be probed. */
    private Checklist getOwned(DeviceId deviceId, Long checklistId) {
        return checklistRepository.findByIdAndDeviceId(checklistId, deviceId.value())
                .orElseThrow(() -> ApiException.notFound("CHECKLIST_NOT_FOUND", "체크리스트를 찾을 수 없습니다."));
    }

    private static ChecklistItem getItem(Checklist checklist, Long itemId) {
        return checklist.findItem(itemId)
                .orElseThrow(() -> ApiException.notFound("CHECKLIST_ITEM_NOT_FOUND", "준비물을 찾을 수 없습니다."));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
