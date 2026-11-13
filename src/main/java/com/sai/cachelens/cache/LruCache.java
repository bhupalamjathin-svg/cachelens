package com.sai.cachelens.cache;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Least-Recently-Used: evicts the key that has gone the longest without being read or written. */
public class LruCache implements EvictionCache {

    private final int capacity;
    // accessOrder=true: iteration order goes from least-recently to most-recently accessed
    private final LinkedHashMap<String, String> map = new LinkedHashMap<>(16, 0.75f, true);

    public LruCache(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        this.capacity = capacity;
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(map.get(key));
    }

    @Override
    public Optional<String> put(String key, String value) {
        if (map.containsKey(key)) {
            map.put(key, value); // refreshes recency
            return Optional.empty();
        }
        String evicted = null;
        if (map.size() >= capacity) {
            Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
            evicted = it.next().getKey();
            it.remove();
        }
        map.put(key, value);
        return Optional.ofNullable(evicted);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public List<String> evictionOrder() {
        return new ArrayList<>(map.keySet());
    }

    @Override
    public String policyName() {
        return "LRU";
    }
}
