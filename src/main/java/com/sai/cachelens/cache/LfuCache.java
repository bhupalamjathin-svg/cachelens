package com.sai.cachelens.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Least-Frequently-Used: evicts the key with the fewest accesses.
 * Ties are broken by recency (the least-recently-used key among the lowest frequency goes first).
 * All operations are O(1) except {@link #evictionOrder()}, which is only used for display.
 */
public class LfuCache implements EvictionCache {

    private final int capacity;
    private final Map<String, String> values = new HashMap<>();
    private final Map<String, Integer> freq = new HashMap<>();
    // frequency -> keys with that frequency, in insertion (recency) order
    private final Map<Integer, LinkedHashSet<String>> buckets = new HashMap<>();
    private int minFreq = 0;

    public LfuCache(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        this.capacity = capacity;
    }

    @Override
    public Optional<String> get(String key) {
        if (!values.containsKey(key)) {
            return Optional.empty();
        }
        touch(key);
        return Optional.of(values.get(key));
    }

    @Override
    public Optional<String> put(String key, String value) {
        if (values.containsKey(key)) {
            values.put(key, value);
            touch(key);
            return Optional.empty();
        }
        String evicted = null;
        if (values.size() >= capacity) {
            LinkedHashSet<String> lowest = buckets.get(minFreq);
            evicted = lowest.iterator().next();
            lowest.remove(evicted);
            if (lowest.isEmpty()) {
                buckets.remove(minFreq);
            }
            values.remove(evicted);
            freq.remove(evicted);
        }
        values.put(key, value);
        freq.put(key, 1);
        buckets.computeIfAbsent(1, f -> new LinkedHashSet<>()).add(key);
        minFreq = 1;
        return Optional.ofNullable(evicted);
    }

    private void touch(String key) {
        int f = freq.get(key);
        LinkedHashSet<String> bucket = buckets.get(f);
        bucket.remove(key);
        if (bucket.isEmpty()) {
            buckets.remove(f);
            if (minFreq == f) {
                minFreq = f + 1;
            }
        }
        freq.put(key, f + 1);
        buckets.computeIfAbsent(f + 1, x -> new LinkedHashSet<>()).add(key);
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public void clear() {
        values.clear();
        freq.clear();
        buckets.clear();
        minFreq = 0;
    }

    @Override
    public List<String> evictionOrder() {
        List<String> order = new ArrayList<>();
        new TreeMap<>(buckets).values().forEach(order::addAll);
        return order;
    }

    @Override
    public String policyName() {
        return "LFU";
    }
}
