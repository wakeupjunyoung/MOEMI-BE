package com.example.moemi.lifeindex;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LifeIndexTypeTest {

    @ParameterizedTest
    @CsvSource({
            "0, 낮음, GOOD",
            "2, 낮음, GOOD",
            "3, 보통, NORMAL",
            "5, 보통, NORMAL",
            "6, 높음, CAUTION",
            "7, 높음, CAUTION",
            "8, 매우높음, CAUTION",
            "10, 매우높음, CAUTION",
            "11, 위험, CAUTION",
    })
    void uvGrades(double value, String label, Level level) {
        assertThat(LifeIndexType.UV.grade(value)).isEqualTo(new IndexGrade(label, level));
    }

    @ParameterizedTest
    @CsvSource({
            "25, 낮음, GOOD",
            "50, 보통, NORMAL",
            "75, 높음, CAUTION",
            "100, 매우높음, CAUTION",
    })
    void airStagnationGrades(double value, String label, Level level) {
        assertThat(LifeIndexType.AIR_STAGNATION.grade(value)).isEqualTo(new IndexGrade(label, level));
    }
}
