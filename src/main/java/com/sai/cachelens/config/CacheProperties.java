package com.sai.cachelens.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cache settings, bound from application.yml, which in turn reads the environment variables
 * CACHE_POLICY and CACHE_CAPACITY (supplied by Ansible's env file or the Kubernetes ConfigMap).
 *
 * Validation is deliberate: an invalid value (e.g. CACHE_CAPACITY=0) makes the app refuse to start.
 * That is what powers the rollback demo — a bad config rollout never becomes Ready, and
 * `kubectl rollout undo` brings the last good revision back.
 */
@Validated
@ConfigurationProperties(prefix = "cachelens")
public record CacheProperties(
        @NotNull Policy policy,
        @Min(1) @Max(100_000) int capacity) {

    public enum Policy { LRU, LFU }
}
