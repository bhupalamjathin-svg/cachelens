package com.sai.cachelens.api;

import com.sai.cachelens.cache.CacheService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CacheLens REST API.
 *
 *   GET    /                  service info: version + which pod answered (shows load-balancing across replicas)
 *   PUT    /api/cache/{key}   store a value  -> reports which key (if any) was evicted
 *   GET    /api/cache/{key}   read a value   -> 200 on hit, 404 on miss
 *   GET    /api/stats         hit rate, evictions, and the current eviction order
 *   DELETE /api/cache         clear the cache (handy between demo runs)
 */
@RestController
public class CacheController {

    private final CacheService cache;
    private final String version;
    private final String instance;

    public CacheController(CacheService cache, ObjectProvider<BuildProperties> build) {
        this.cache = cache;
        BuildProperties props = build.getIfAvailable();
        this.version = props != null ? props.getVersion() : "dev";
        this.instance = hostname();
    }

    public record PutRequest(@NotBlank String value) {}

    @GetMapping("/")
    public Map<String, Object> info() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app", "cachelens");
        body.put("version", version);
        body.put("instance", instance);
        body.put("policy", cache.policyName());
        body.put("capacity", cache.capacity());
        return body;
    }

    @PutMapping("/api/cache/{key}")
    public Map<String, Object> put(@PathVariable String key, @Valid @RequestBody PutRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("stored", key);
        body.put("evicted", cache.put(key, req.value()).orElse(null));
        body.put("size", cache.size());
        body.put("instance", instance);
        return body;
    }

    @GetMapping("/api/cache/{key}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String key) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("key", key);
        body.put("instance", instance);
        return cache.get(key)
                .map(v -> {
                    body.put("hit", true);
                    body.put("value", v);
                    return ResponseEntity.ok(body);
                })
                .orElseGet(() -> {
                    body.put("hit", false);
                    return ResponseEntity.status(404).body(body);
                });
    }

    @GetMapping("/api/stats")
    public Map<String, Object> stats() {
        Map<String, Object> body = cache.stats();
        body.put("instance", instance);
        return body;
    }

    @DeleteMapping("/api/cache")
    public ResponseEntity<Void> clear() {
        cache.clear();
        return ResponseEntity.noContent().build();
    }

    private static String hostname() {
        String env = System.getenv("HOSTNAME"); // Kubernetes sets this to the pod name
        if (env != null && !env.isBlank()) {
            return env;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
