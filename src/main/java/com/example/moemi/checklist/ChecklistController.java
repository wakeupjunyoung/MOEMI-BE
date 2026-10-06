package com.example.moemi.checklist;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.moemi.checklist.dto.AddItemRequest;
import com.example.moemi.checklist.dto.ChecklistResponse;
import com.example.moemi.checklist.dto.OpenChecklistRequest;
import com.example.moemi.checklist.dto.UpdateItemRequest;
import com.example.moemi.common.web.DeviceId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Every endpoint requires the {@value DeviceId#HEADER} header and only sees that device's checklists.
 * Mutations return the whole checklist so the progress (n / total) can be redrawn at once.
 */
@RestController
@RequestMapping("/api/checklists")
@RequiredArgsConstructor
public class ChecklistController {

    private final ChecklistService checklistService;

    @PostMapping
    public ChecklistResponse open(DeviceId deviceId, @Valid @RequestBody OpenChecklistRequest request) {
        return checklistService.open(deviceId, request);
    }

    @GetMapping
    public List<ChecklistResponse> list(DeviceId deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return checklistService.findByDate(deviceId, date);
    }

    @GetMapping("/{checklistId}")
    public ChecklistResponse get(DeviceId deviceId, @PathVariable Long checklistId) {
        return checklistService.get(deviceId, checklistId);
    }

    @PostMapping("/{checklistId}/recommendations")
    public ChecklistResponse refreshRecommendations(DeviceId deviceId, @PathVariable Long checklistId,
            @RequestParam(required = false) String regionCode) {
        return checklistService.refreshRecommendations(deviceId, checklistId, regionCode);
    }

    @PostMapping("/{checklistId}/items")
    public ChecklistResponse addItem(DeviceId deviceId, @PathVariable Long checklistId,
            @Valid @RequestBody AddItemRequest request) {
        return checklistService.addItem(deviceId, checklistId, request);
    }

    @PatchMapping("/{checklistId}/items/{itemId}")
    public ChecklistResponse updateItem(DeviceId deviceId, @PathVariable Long checklistId, @PathVariable Long itemId,
            @Valid @RequestBody UpdateItemRequest request) {
        return checklistService.updateItem(deviceId, checklistId, itemId, request);
    }

    @DeleteMapping("/{checklistId}/items/{itemId}")
    public ChecklistResponse removeItem(DeviceId deviceId, @PathVariable Long checklistId, @PathVariable Long itemId) {
        return checklistService.removeItem(deviceId, checklistId, itemId);
    }
}
