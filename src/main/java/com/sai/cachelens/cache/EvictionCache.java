package com.sai.cachelens.cache;

import java.util.List;
import java.util.Optional;

/**
 * A fixed-capacity key/value cache with a pluggable eviction policy.
 * Implementations are NOT thread-safe; {@link com.sai.cachelens.cache.CacheService} serialises access.
 */
public interface EvictionCache {

    /** Looks up a key. A hit counts as an "access" for the eviction policy. */
    Optional<String> get(String key);

    /**
     * Inserts or updates a key.
     *
     * @return the key that was evicted to make room, if any
     */
    Optional<String> put(String key, String value);

    int size();

    int capacity();

    void clear();

    /** Keys ordered from "evicted next" to "evicted last". */
    List<String> evictionOrder();

    /** Short policy name, e.g. "LRU". */
    String policyName();
}
