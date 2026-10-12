package com.example.moemi.common.web;

/**
 * Identifies the app installation that owns per-user data, sent as the {@value #HEADER} header.
 * Stands in for a user account until a login feature exists.
 */
public record DeviceId(String value) {

    public static final String HEADER = "X-Device-Id";
    public static final int MAX_LENGTH = 64;
}
