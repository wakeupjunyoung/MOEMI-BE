package com.example.moemi.lifeindex;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Easy-to-read status shown next to each index. Declared from best to worst so that
 * {@code compareTo} can pick the worst level of a day.
 */
@Getter
@RequiredArgsConstructor
public enum Level {
    GOOD("양호"),
    NORMAL("보통"),
    CAUTION("주의");

    private final String label;
}
