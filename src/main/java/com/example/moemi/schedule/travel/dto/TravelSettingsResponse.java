package com.example.moemi.schedule.travel.dto;

import com.example.moemi.schedule.travel.TravelMode;
import com.example.moemi.schedule.travel.TravelPreference;
import com.example.moemi.schedule.travel.TravelTimeService;

/**
 * @param alwaysWalkMinutes places within this many minutes' walk are always walked
 */
public record TravelSettingsResponse(TravelMode defaultMode, int bufferMinutes, int roundUpMinutes, int alwaysWalkMinutes) {

    public static TravelSettingsResponse from(TravelPreference preference) {
        return new TravelSettingsResponse(preference.getDefaultMode(), preference.getBufferMinutes(),
                preference.getRoundUpMinutes(), TravelTimeService.ALWAYS_WALK_MINUTES);
    }
}
