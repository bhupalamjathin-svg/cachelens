package com.sai.cachelens.cache;

import com.sai.cachelens.config.CacheProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Thread-safe facade over the configured eviction cache, plus hit/miss/eviction metrics. */
@Service
public class CacheService {

    private final EvictionCache cache;
    private final Counter hits;
    private final Counter misses;
    private final Counter evictions;

    public CacheService(CacheProperties props, MeterRegistry registry) {
        this.cache = switch (props.policy()) {
            case LRU -> new LruCache(props.capacity());
            case LFU -> new LfuCache(props.capacity());
        };
        String policy = cache.policyName();
        this.hits = Counter.builder("cachelens.hits").tag("policy", policy).register(registry);
        this.misses = Counter.builder("cachelens.misses").tag("policy", policy).register(registry);
        this.evictions = Counter.builder("cachelens.evictions").tag("policy", policy).register(registry);
        Gauge.builder("cachelens.size", this, CacheService::size).tag("policy", policy).register(registry);
    }

    public synchronized Optional<String> get(String key) {
        Optional<String> value = cache.get(key);
        (value.isPresent() ? hits : misses).increment();
        return value;
    }

    public synchronized Optional<String> put(String key, String value) {
        Optional<String> evicted = cache.put(key, value);
        evicted.ifPresent(k -> evictions.increment());
        return evicted;
    }

    public synchronized void clear() {
        cache.clear();
    }

    public synchronized int size() {
        return cache.size();
    }

    public synchronized Map<String, Object> stats() {
        double h = hits.count();
        double m = misses.count();
        double total = h + m;
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("policy", cache.policyName());
        s.put("capacity", cache.capacity());
        s.put("size", cache.size());
        s.put("hits", (long) h);
        s.put("misses", (long) m);
        s.put("hitRate", total == 0 ? 0.0 : Math.round(h / total * 1000) / 1000.0);
        s.put("evictions", (long) evictions.count());
        s.put("evictionOrder", cache.evictionOrder());
        return s;
    }

    public String policyName() {
        return cache.policyName();
    }

    public int capacity() {
        return cache.capacity();
    }
}
