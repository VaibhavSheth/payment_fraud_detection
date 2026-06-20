package com.frauddetection.producer;

import com.frauddetection.model.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionProducer {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Value("${kafka.topic.transactions-raw}")
    private String topic;

    public void send(TransactionEvent event) {
        kafkaTemplate.send(topic, event.getUserId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[PRODUCER] Failed to send txn={} user={} error={}",
                                event.getTxnId(), event.getUserId(), ex.getMessage());
                    } else {
                        log.debug("[PRODUCER] Sent txn={} user={} partition={}",
                                event.getTxnId(), event.getUserId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
