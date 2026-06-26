package com.frauddetection.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "txn_id", nullable = false)
    private String txnId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "sender_vpa")
    private String senderVpa;

    @Column(name = "receiver_vpa")
    private String receiverVpa;

    @Column(name = "channel")
    private String channel;

    @Column(name = "decision", nullable = false)
    private String decision;

    @Column(name = "triggered_rule")
    private String triggeredRule;

    @Column(name = "reason")
    private String reason;

    @Column(name = "rules_evaluated", nullable = false)
    private int rulesEvaluated;

    @Column(name = "txn_count")
    private Integer txnCount;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "decided_at", nullable = false)
    private LocalDateTime decidedAt;

    @Column(name = "overridden")
    private boolean overridden;

    @Column(name = "override_reason")
    private String overrideReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
