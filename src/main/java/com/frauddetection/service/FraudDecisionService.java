package com.frauddetection.service;

import com.frauddetection.model.AuditLog;
import com.frauddetection.model.FraudDecision;
import com.frauddetection.repository.AuditLogRepository;
import com.frauddetection.rules.FraudRule;
import com.frauddetection.rules.RuleEngine;
import com.frauddetection.streams.WindowedTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDecisionService {

    private final RuleEngine ruleEngine;
    private final RuleLoaderService ruleLoaderService;
    private final IdempotencyService idempotencyService;
    private final AuditLogRepository auditLogRepository;
    private final KafkaTemplate<String, FraudDecision> decisionKafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${kafka.topic.fraud-alerts}")
    private String fraudAlertsTopic;

    @Value("${kafka.topic.allowed-transactions}")
    private String allowedTransactionsTopic;

    @Transactional
    public void process(WindowedTransaction windowedTxn) {
        if (windowedTxn == null || windowedTxn.getCurrentEvent() == null) return;

        String idempotencyKey = windowedTxn.getCurrentEvent().getIdempotencyKey();

        if (idempotencyService.isAlreadyProcessed(idempotencyKey)) {
            return;
        }

        List<FraudRule> rules = ruleLoaderService.getRules();
        Set<String> knownPayees = getKnownPayees(windowedTxn.getUserId());

        FraudDecision decision = ruleEngine.evaluate(windowedTxn, rules, knownPayees);

        persistAuditLog(decision, windowedTxn);
        routeDecision(decision);

        if (decision.getDecision() == FraudDecision.DecisionType.ALLOW) {
            updateKnownPayees(windowedTxn.getUserId(),
                    windowedTxn.getCurrentEvent().getReceiverVpa());
        }

        log.info("[DECISION] txnId={} user={} decision={} rule={}",
                decision.getTxnId(), decision.getUserId(),
                decision.getDecision(), decision.getTriggeredRule());
    }

    private void routeDecision(FraudDecision decision) {
        String topic = decision.getDecision() == FraudDecision.DecisionType.BLOCK
                ? fraudAlertsTopic
                : allowedTransactionsTopic;

        decisionKafkaTemplate.send(topic, decision.getUserId(), decision)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[DECISION] Failed to publish decision for txnId={}: {}",
                                decision.getTxnId(), ex.getMessage());
                    }
                });
    }

    private void persistAuditLog(FraudDecision decision, WindowedTransaction window) {
        AuditLog log = AuditLog.builder()
                .txnId(decision.getTxnId())
                .userId(decision.getUserId())
                .amount(decision.getAmount())
                .senderVpa(window.getCurrentEvent().getSenderVpa())
                .receiverVpa(window.getCurrentEvent().getReceiverVpa())
                .channel(window.getCurrentEvent().getChannel().name())
                .decision(decision.getDecision().name())
                .triggeredRule(decision.getTriggeredRule())
                .reason(decision.getReason())
                .rulesEvaluated(decision.getRulesEvaluated())
                .txnCount(window.getTxnCount())
                .totalAmount(window.getTotalAmount())
                .decidedAt(LocalDateTime.now())
                .overridden(false)
                .build();

        auditLogRepository.save(log);
    }

    @SuppressWarnings("unchecked")
    private Set<String> getKnownPayees(String userId) {
        try {
            Object cached = redisTemplate.opsForValue().get("fraud:known-payees:" + userId);
            if (cached instanceof Set) return (Set<String>) cached;
        } catch (Exception e) {
            // fallback to empty set
        }
        return new HashSet<>();
    }

    private void updateKnownPayees(String userId, String receiverVpa) {
        if (receiverVpa == null) return;
        try {
            String key = "fraud:known-payees:" + userId;
            redisTemplate.opsForSet().add(key, receiverVpa);
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("[DECISION] Failed to update known payees for user={}: {}", userId, e.getMessage());
        }
    }
}
