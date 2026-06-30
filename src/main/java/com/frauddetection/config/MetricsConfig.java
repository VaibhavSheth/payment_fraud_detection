package com.frauddetection.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter transactionTotalCounter(MeterRegistry registry) {
        return Counter.builder("fraud.transactions.total")
                .description("Total transactions evaluated by fraud detection")
                .register(registry);
    }

    @Bean
    public Timer ruleEvaluationTimer(MeterRegistry registry) {
        return Timer.builder("fraud.rule.evaluation.duration")
                .description("Time taken to evaluate fraud rules")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }
}
