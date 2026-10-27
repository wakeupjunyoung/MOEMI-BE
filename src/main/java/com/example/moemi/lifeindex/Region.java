package com.example.moemi.lifeindex;

import java.util.Arrays;
import java.util.Optional;

import com.example.moemi.common.error.ApiException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Regions the life index calendar supports.
 * Codes are KMA area numbers (areaNo) from the 2026-07-01 code table, where Gwangju districts
 * belong to 전남광주통합특별시 (12xxxxxxxx).
 */
@Getter
@RequiredArgsConstructor
public enum Region {
    GWANGJU_GWANGSAN("1233000000", "광주 광산구"),
    GWANGJU_BUK("1230000000", "광주 북구"),
    GWANGJU_SEO("1224000000", "광주 서구"),
    GWANGJU_NAM("1227000000", "광주 남구"),
    GWANGJU_DONG("1221000000", "광주 동구");

    private final String code;
    private final String displayName;

    public static Optional<Region> fromCode(String code) {
        return Arrays.stream(values())
                .filter(region -> region.code.equals(code))
                .findFirst();
    }

    /** Resolves a code sent by the client, rejecting a missing or unknown one with 400. */
    public static Region require(String code) {
        if (code == null || code.isBlank()) {
            throw ApiException.badRequest("REGION_REQUIRED", "지역을 먼저 설정해 주세요.");
        }
        return fromCode(code)
                .orElseThrow(() -> ApiException.badRequest("UNKNOWN_REGION", "지원하지 않는 지역입니다: " + code));
    }
}
