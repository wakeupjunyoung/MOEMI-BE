package com.example.moemi.lifeindex.dto;

import com.example.moemi.lifeindex.Region;

public record RegionResponse(String code, String name) {

    public static RegionResponse from(Region region) {
        return new RegionResponse(region.getCode(), region.getDisplayName());
    }
}
