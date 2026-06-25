package com.frauddetection.rules;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fraud_rules")
public class FraudRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_name", nullable = false, unique = true)
    private String ruleName;

    @Column(name = "description")
    private String description;

    @Column(name = "rule_type", nullable = false)
    private String ruleType;

    @Column(name = "threshold", nullable = false)
    private BigDecimal threshold;

    @Column(name = "window_seconds", nullable = false)
    private int windowSeconds;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
