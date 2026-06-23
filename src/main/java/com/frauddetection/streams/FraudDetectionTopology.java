package com.frauddetection.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.model.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.math.BigDecimal;
import java.time.Duration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FraudDetectionTopology {

    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.transactions-raw}")
    private String inputTopic;

    @Value("${kafka.topic.windowed-transactions}")
    private String outputTopic;

    @Bean
    public KStream<String, TransactionEvent> fraudDetectionStream(StreamsBuilder streamsBuilder) {

        JsonSerde<TransactionEvent> txnSerde = new JsonSerde<>(TransactionEvent.class, objectMapper);
        JsonSerde<WindowedTransaction> windowSerde = new JsonSerde<>(WindowedTransaction.class, objectMapper);

        SlidingWindows slidingWindow = SlidingWindows.ofTimeDifferenceAndGrace(
                Duration.ofSeconds(60),
                Duration.ofSeconds(5)
        );

        Materialized<String, WindowedTransaction, WindowStore<Bytes, byte[]>> store =
                Materialized.<String, WindowedTransaction, WindowStore<Bytes, byte[]>>
                        as("fraud-window-store")
                        .withRetention(Duration.ofMinutes(2))
                        .withKeySerde(Serdes.String())
                        .withValueSerde(windowSerde);

        KStream<String, TransactionEvent> stream = streamsBuilder
                .stream(inputTopic, Consumed.with(Serdes.String(), txnSerde));

        stream
                .filter((key, event) -> {
                    if (event == null || event.getTxnId() == null) {
                        log.warn("[TOPOLOGY] Dropping null event");
                        return false;
                    }
                    if (event.getAmount() == null || event.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        log.warn("[TOPOLOGY] Dropping event with invalid amount: {}", event.getTxnId());
                        return false;
                    }
                    return true;
                })
                .groupByKey(Grouped.with(Serdes.String(), txnSerde))
                .windowedBy(slidingWindow)
                .aggregate(
                        WindowedTransaction::new,
                        (userId, newEvent, currentWindow) -> aggregate(userId, newEvent, currentWindow),
                        store
                )
                .toStream()
                .map((windowedKey, windowedTxn) -> {
                    if (windowedTxn != null) {
                        windowedTxn.setUserId(windowedKey.key());
                        windowedTxn.setWindowStartMs(windowedKey.window().start());
                        windowedTxn.setWindowEndMs(windowedKey.window().end());
                        log.debug("[TOPOLOGY] Window update user={} txnCount={} totalAmount={}",
                                windowedKey.key(), windowedTxn.getTxnCount(), windowedTxn.getTotalAmount());
                    }
                    return KeyValue.pair(windowedKey.key(), windowedTxn);
                })
                .filter((key, value) -> value != null)
                .to(outputTopic, Produced.with(Serdes.String(), windowSerde));

        return stream;
    }

    private WindowedTransaction aggregate(String userId, TransactionEvent newEvent, WindowedTransaction current) {
        if (current.getUserId() == null) {
            current.setUserId(userId);
            current.setTotalAmount(BigDecimal.ZERO);
            current.setWindowStartMs(System.currentTimeMillis());
        }

        current.setTxnCount(current.getTxnCount() + 1);
        current.setTotalAmount(current.getTotalAmount().add(newEvent.getAmount()));

        if (newEvent.getReceiverVpa() != null) {
            current.getPayeesSeen().add(newEvent.getReceiverVpa());
            current.setDistinctPayees(current.getPayeesSeen().size());
        }

        if (newEvent.getStatus() == TransactionEvent.TxnStatus.FAILED) {
            current.setFailedTxnCount(current.getFailedTxnCount() + 1);
        }

        current.setCurrentEvent(newEvent);
        current.setWindowEndMs(System.currentTimeMillis());

        return current;
    }
}
