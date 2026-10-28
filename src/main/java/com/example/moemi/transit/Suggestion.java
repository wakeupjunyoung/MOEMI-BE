package com.example.moemi.transit;

/**
 * A similar keyword that does have results, offered when a search finds nothing.
 */
public record Suggestion(String keyword, int resultCount) {
}
