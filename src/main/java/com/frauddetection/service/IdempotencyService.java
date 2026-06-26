package com.frauddetection.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String PREFIX = "fraud:idempotency:";
    private static final long TTL_HOURS = 24;

    private final RedisTemplate<String, Object> redisTemplate;

    public boolean isAlreadyProcessed(String idempotencyKey) {
        String redisKey = PREFIX + idempotencyKey;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "processed", TTL_HOURS, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(isNew)) {
            log.info("[IDEMPOTENCY] Duplicate detected, skipping: {}", idempotencyKey);
            return true;
        }
        return false;
    }
}
