package com.frauddetection.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.rules.FraudRule;
import com.frauddetection.rules.RuleRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleLoaderService {

    private static final String RULES_CACHE_KEY = "fraud:rules:all";

    private final RuleRepository ruleRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private List<FraudRule> lastKnownRules = new ArrayList<>();

    @CircuitBreaker(name = "rule-db", fallbackMethod = "reloadRulesFallback")
    @Scheduled(fixedDelayString = "${rule.reload.interval.ms:30000}")
    public void reloadRules() {
        List<FraudRule> rules = ruleRepository.findByIsActiveTrueOrderByPriorityAsc();
        lastKnownRules = rules;
        redisTemplate.opsForValue().set(RULES_CACHE_KEY, rules, 60, TimeUnit.SECONDS);
        log.info("[RULES] Reloaded {} active rules into Redis", rules.size());
    }

    public void reloadRulesFallback(Exception ex) {
        log.warn("[RULES] DB unavailable, using {} in-memory rules. Error: {}",
                lastKnownRules.size(), ex.getMessage());
    }

    public List<FraudRule> getRules() {
        try {
            Object cached = redisTemplate.opsForValue().get(RULES_CACHE_KEY);
            if (cached != null) {
                List<?> rawList = objectMapper.convertValue(cached, List.class);
                List<FraudRule> rules = new ArrayList<>();
                for (Object item : rawList) {
                    rules.add(objectMapper.convertValue(item, FraudRule.class));
                }
                return rules;
            }
        } catch (Exception e) {
            log.warn("[RULES] Failed to read from Redis, falling back to in-memory: {}", e.getMessage());
        }
        return lastKnownRules;
    }

    public List<FraudRule> getLastKnownRules() {
        return lastKnownRules;
    }
}
