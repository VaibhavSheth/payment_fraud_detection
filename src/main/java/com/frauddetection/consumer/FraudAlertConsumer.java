package com.frauddetection.consumer;

import com.frauddetection.model.FraudDecision;
import com.frauddetection.service.FraudDecisionService;
import com.frauddetection.streams.WindowedTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudAlertConsumer {

    private final FraudDecisionService fraudDecisionService;

    @KafkaListener(topics = "${kafka.topic.windowed-transactions}", groupId = "fraud-decision-group")
    public void consumeWindowedTransaction(WindowedTransaction windowedTxn, Acknowledgment ack) {
        try {
            fraudDecisionService.process(windowedTxn);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[CONSUMER] Failed to process windowed transaction: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${kafka.topic.fraud-alerts}", groupId = "fraud-alert-log-group")
    public void consumeFraudAlert(FraudDecision decision, Acknowledgment ack) {
        log.warn("[ALERT] FRAUD DETECTED — txnId={} user={} rule={} amount={} reason={}",
                decision.getTxnId(),
                decision.getUserId(),
                decision.getTriggeredRule(),
                decision.getAmount(),
                decision.getReason());
        ack.acknowledge();
    }
}
