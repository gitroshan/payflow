package com.payflow.orchestrator.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Idempotency guard backed by Redis. Clients send an Idempotency-Key header;
 * the first request stores key -> paymentId with SET NX, so any retry of the
 * same logical request resolves to the original payment instead of creating a
 * duplicate charge.
 */
@Service
public class IdempotencyService {

    private static final Duration TTL = Duration.ofHours(24);
    private static final String PREFIX = "idem:payment:";

    private final StringRedisTemplate redis;

    public IdempotencyService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** @return true if this key was newly reserved; false if it already existed. */
    public boolean reserve(String key, String paymentId) {
        Boolean ok = redis.opsForValue().setIfAbsent(PREFIX + key, paymentId, TTL);
        return Boolean.TRUE.equals(ok);
    }

    public String lookup(String key) {
        return redis.opsForValue().get(PREFIX + key);
    }
}
