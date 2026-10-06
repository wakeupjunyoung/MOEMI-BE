package com.example.moemi.checklist;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.moemi.lifeindex.DailyLifeIndex;
import com.example.moemi.lifeindex.LifeIndexType;
import com.example.moemi.lifeindex.Level;

/**
 * Maps a day's life indices to items worth bringing. A rule fires when its index is "높음" or worse
 * ({@link Level#CAUTION}).
 */
final class PackingRecommender {

    private record Rule(String reason, List<String> items) {
    }

    private static final Map<LifeIndexType, Rule> RULES = new EnumMap<>(Map.of(
            LifeIndexType.UV, new Rule("자외선 높음", List.of("선크림", "모자", "선글라스")),
            LifeIndexType.AIR_STAGNATION, new Rule("대기정체 높음", List.of("마스크"))));

    private PackingRecommender() {
    }

    static List<Recommendation> recommend(DailyLifeIndex day) {
        Map<String, Recommendation> byName = new LinkedHashMap<>();
        RULES.forEach((type, rule) -> {
            boolean triggered = day.find(type)
                    .map(index -> index.grade().level() == Level.CAUTION)
                    .orElse(false);
            if (triggered) {
                rule.items().forEach(item -> byName.putIfAbsent(item, new Recommendation(item, rule.reason())));
            }
        });
        return List.copyOf(byName.values());
    }
}
