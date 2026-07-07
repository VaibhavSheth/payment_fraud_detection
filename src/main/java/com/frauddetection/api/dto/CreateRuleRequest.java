package com.frauddetection.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateRuleRequest {

    @NotBlank(message = "ruleName is required")
    private String ruleName;

    private String description;

    @NotBlank(message = "ruleType is required")
    private String ruleType;

    @NotNull(message = "threshold is required")
    @Positive(message = "threshold must be greater than 0")
    private BigDecimal threshold;

    @Min(value = 0, message = "windowSeconds cannot be negative")
    private int windowSeconds;

    @Positive(message = "priority must be greater than 0")
    private int priority;
}
