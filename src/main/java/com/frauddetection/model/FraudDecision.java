package com.frauddetection.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudDecision {

    private String txnId;
    private String userId;
    private DecisionType decision;
    private String triggeredRule;
    private String reason;
    private BigDecimal amount;
    private long decidedAt;
    private int rulesEvaluated;

    public enum DecisionType {
        ALLOW, BLOCK
    }
}
