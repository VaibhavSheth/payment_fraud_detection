package com.frauddetection.api.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateRuleRequest {

    @Positive(message = "threshold must be greater than 0")
    private BigDecimal threshold;

    private String description;

    @Positive(message = "priority must be greater than 0")
    private Integer priority;
}
