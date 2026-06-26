package com.frauddetection.rules;

import com.frauddetection.model.FraudDecision;
import com.frauddetection.model.FraudDecision.DecisionType;
import com.frauddetection.streams.WindowedTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class RuleEngine {

    public FraudDecision evaluate(WindowedTransaction window, List<FraudRule> rules, Set<String> knownPayees) {
        int rulesChecked = 0;

        for (FraudRule rule : rules) {
            if (!rule.isActive()) continue;
            rulesChecked++;

            boolean triggered = switch (rule.getRuleName()) {
                case "VELOCITY_ABUSE" ->
                        window.getTxnCount() > rule.getThreshold().intValue();

                case "LARGE_AMOUNT_NEW_PAYEE" ->
                        window.getCurrentEvent().getAmount()
                                .compareTo(rule.getThreshold()) > 0
                                && !knownPayees.contains(window.getCurrentEvent().getReceiverVpa());

                case "NEW_DEVICE_HIGH_AMOUNT" ->
                        window.getCurrentEvent().isNewDevice()
                                && window.getCurrentEvent().getAmount()
                                .compareTo(rule.getThreshold()) > 0;

                case "FAILED_ATTEMPTS" ->
                        window.getFailedTxnCount() > rule.getThreshold().intValue();

                default -> false;
            };

            if (triggered) {
                log.debug("[RULE ENGINE] Rule {} triggered for user={} txnId={}",
                        rule.getRuleName(), window.getUserId(),
                        window.getCurrentEvent().getTxnId());

                return FraudDecision.builder()
                        .txnId(window.getCurrentEvent().getTxnId())
                        .userId(window.getUserId())
                        .decision(DecisionType.BLOCK)
                        .triggeredRule(rule.getRuleName())
                        .reason(buildReason(rule, window))
                        .amount(window.getCurrentEvent().getAmount())
                        .decidedAt(System.currentTimeMillis())
                        .rulesEvaluated(rulesChecked)
                        .build();
            }
        }

        return FraudDecision.builder()
                .txnId(window.getCurrentEvent().getTxnId())
                .userId(window.getUserId())
                .decision(DecisionType.ALLOW)
                .amount(window.getCurrentEvent().getAmount())
                .decidedAt(System.currentTimeMillis())
                .rulesEvaluated(rulesChecked)
                .build();
    }

    private String buildReason(FraudRule rule, WindowedTransaction window) {
        return switch (rule.getRuleName()) {
            case "VELOCITY_ABUSE" ->
                    window.getTxnCount() + " transactions in 60s (threshold: " + rule.getThreshold().intValue() + ")";
            case "LARGE_AMOUNT_NEW_PAYEE" ->
                    "Amount " + window.getCurrentEvent().getAmount()
                            + " to first-time payee " + window.getCurrentEvent().getReceiverVpa();
            case "NEW_DEVICE_HIGH_AMOUNT" ->
                    "New device detected with high-value transfer " + window.getCurrentEvent().getAmount();
            case "FAILED_ATTEMPTS" ->
                    window.getFailedTxnCount() + " failed attempts in 5 minutes";
            default -> "Rule: " + rule.getRuleName();
        };
    }
}
