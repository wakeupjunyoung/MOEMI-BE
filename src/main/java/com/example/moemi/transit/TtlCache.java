package com.example.moemi.transit;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Reuses TAGO responses for a while, so that everyone looking at the same stop costs one call.
 * Concurrent misses for one key wait for a single call instead of each calling TAGO.
 */
final class TtlCache<K, V> {

    private static final int MAX_ENTRIES = 5_000;

    private final Duration ttl;
    private final Clock clock;
    private final Map<K, Cached<V>> entries = new ConcurrentHashMap<>();
    private final Map<K, CompletableFuture<Cached<V>>> inFlight = new ConcurrentHashMap<>();

    TtlCache(Duration ttl, Clock clock) {
        this.ttl = ttl;
        this.clock = clock;
    }

    /**
     * @param allowStale on failure, return the last value (marked stale) instead of throwing
     * @throws TransitFetchException when TAGO fails and nothing usable is cached
     */
    Cached<V> get(K key, Supplier<V> loader, boolean allowStale) {
        Cached<V> cached = entries.get(key);
        if (cached != null && cached.fetchedAt().plus(ttl).isAfter(now())) {
            return cached;
        }
        try {
            return load(key, loader);
        } catch (TransitFetchException e) {
            if (allowStale && cached != null) {
                return cached.asStale();
            }
            throw e;
        }
    }

    private Cached<V> load(K key, Supplier<V> loader) {
        CompletableFuture<Cached<V>> mine = new CompletableFuture<>();
        CompletableFuture<Cached<V>> running = inFlight.putIfAbsent(key, mine);
        if (running != null) {
            try {
                return running.join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof RuntimeException cause) {
                    throw cause;
                }
                throw e;
            }
        }
        try {
            Cached<V> fetched = new Cached<>(loader.get(), now(), false);
            if (entries.size() >= MAX_ENTRIES) {
                evictExpired();
            }
            entries.put(key, fetched);
            mine.complete(fetched);
            return fetched;
        } catch (RuntimeException e) {
            mine.completeExceptionally(e);
            throw e;
        } finally {
            inFlight.remove(key, mine);
        }
    }

    private void evictExpired() {
        LocalDateTime limit = now().minus(ttl);
        entries.values().removeIf(entry -> !entry.fetchedAt().isAfter(limit));
        if (entries.size() >= MAX_ENTRIES) {
            entries.clear();
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * @param fetchedAt when TAGO was actually called, shown as the lookup time (조회 시각)
     * @param stale     TAGO failed and this is an older value
     */
    record Cached<V>(V value, LocalDateTime fetchedAt, boolean stale) {

        Cached<V> asStale() {
            return new Cached<>(value, fetchedAt, true);
        }
    }
}
