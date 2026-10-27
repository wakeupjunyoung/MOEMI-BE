package com.example.moemi.lifeindex;

import lombok.Getter;

/**
 * Indices offered by KMA's living weather index service (LivingWthrIdxServiceV5).
 * As of 2026-08 the service only provides these two operations.
 */
@Getter
public enum LifeIndexType {

    /** 0~2 낮음, 3~5 보통, 6~7 높음, 8~10 매우높음, 11+ 위험. Forecast h0~h75 every 3 hours. */
    UV("자외선지수", null, "getUVIdxV5") {
        @Override
        public IndexGrade grade(double value) {
            if (value < 3) {
                return new IndexGrade("낮음", Level.GOOD);
            }
            if (value < 6) {
                return new IndexGrade("보통", Level.NORMAL);
            }
            if (value < 8) {
                return new IndexGrade("높음", Level.CAUTION);
            }
            if (value < 11) {
                return new IndexGrade("매우높음", Level.CAUTION);
            }
            return new IndexGrade("위험", Level.CAUTION);
        }
    },

    /** Values are 25 낮음, 50 보통, 75 높음, 100 매우높음. Forecast h3~h78 every 3 hours. */
    AIR_STAGNATION("대기정체지수", null, "getAirDiffusionIdxV5") {
        @Override
        public IndexGrade grade(double value) {
            // Midpoints between the documented values, in case an in-between value ever arrives.
            if (value < 37.5) {
                return new IndexGrade("낮음", Level.GOOD);
            }
            if (value < 62.5) {
                return new IndexGrade("보통", Level.NORMAL);
            }
            if (value < 87.5) {
                return new IndexGrade("높음", Level.CAUTION);
            }
            return new IndexGrade("매우높음", Level.CAUTION);
        }
    };

    private final String displayName;
    /** Unit of the value, or {@code null} when the index is dimensionless. */
    private final String unit;
    private final String operation;

    LifeIndexType(String displayName, String unit, String operation) {
        this.displayName = displayName;
        this.unit = unit;
        this.operation = operation;
    }

    public abstract IndexGrade grade(double value);
}
