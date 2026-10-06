package com.example.moemi.checklist;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.moemi.lifeindex.DailyLifeIndex;
import com.example.moemi.lifeindex.IndexValue;
import com.example.moemi.lifeindex.LifeIndexType;

class PackingRecommenderTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 6);
    private static final LocalDateTime ISSUED = DATE.atTime(6, 0);

    @Test
    void highUvAndStagnationRecommendTheirItems() {
        DailyLifeIndex day = day(IndexValue.of(LifeIndexType.UV, 6, ISSUED),
                IndexValue.of(LifeIndexType.AIR_STAGNATION, 75, ISSUED));

        assertThat(PackingRecommender.recommend(day)).containsExactly(
                new Recommendation("선크림", "자외선 높음"),
                new Recommendation("모자", "자외선 높음"),
                new Recommendation("선글라스", "자외선 높음"),
                new Recommendation("마스크", "대기정체 높음"));
    }

    @Test
    void normalLevelsRecommendNothing() {
        DailyLifeIndex day = day(IndexValue.of(LifeIndexType.UV, 5, ISSUED),
                IndexValue.of(LifeIndexType.AIR_STAGNATION, 50, ISSUED));

        assertThat(PackingRecommender.recommend(day)).isEmpty();
    }

    private static DailyLifeIndex day(IndexValue... indices) {
        return new DailyLifeIndex(DATE, List.of(indices));
    }
}
