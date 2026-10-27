package com.example.moemi.checklist;

/**
 * An item to bring and the index condition behind it, e.g. ("선크림", "자외선 높음").
 */
public record Recommendation(String name, String reason) {
}
