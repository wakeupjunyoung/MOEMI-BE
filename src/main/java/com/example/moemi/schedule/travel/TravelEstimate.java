package com.example.moemi.schedule.travel;

/**
 * A suggested travel time. It is only a suggestion: the user confirms or edits it before saving.
 *
 * @param mode             the mode the estimate is for; WALK whenever walking takes 10 minutes or less
 * @param requestedMode    the mode asked for (or the user's default)
 * @param forcedWalk       walking was chosen instead of the requested mode because the place is that close
 * @param minutes          estimated minutes for {@code mode}; {@code null} unless OK
 * @param suggestedMinutes {@code minutes} with the user's buffer and rounding applied (25 → 30); use this as the default
 * @param walkMinutes      estimated walking minutes, also given when another mode failed
 * @param distanceM        straight-line distance
 * @param summary          the buses to take, e.g. "송정19 → 1187 (환승 1회)", or "도보"
 */
public record TravelEstimate(
        TravelStatus status,
        TravelMode mode,
        TravelMode requestedMode,
        boolean forcedWalk,
        Integer minutes,
        Integer suggestedMinutes,
        Integer walkMinutes,
        Integer distanceM,
        String summary) {

    static TravelEstimate without(TravelStatus status, TravelMode requestedMode) {
        return new TravelEstimate(status, null, requestedMode, false, null, null, null, null, null);
    }
}
