package com.frauddetection.api;

import com.frauddetection.streams.WindowedTransaction;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    public DebugController(StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
    }

    @GetMapping("/window/{userId}")
    public ResponseEntity<WindowedTransaction> getWindowState(@PathVariable String userId) {
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
        if (streams == null) {
            return ResponseEntity.status(503).build();
        }

        ReadOnlyWindowStore<String, WindowedTransaction> store = streams.store(
                StoreQueryParameters.fromNameAndType(
                        "fraud-window-store",
                        QueryableStoreTypes.windowStore()
                )
        );

        long now = System.currentTimeMillis();
        try (WindowStoreIterator<WindowedTransaction> iterator =
                     store.fetch(userId, Instant.ofEpochMilli(now - 60000), Instant.ofEpochMilli(now))) {
            if (iterator.hasNext()) {
                return ResponseEntity.ok(iterator.next().value);
            }
        }

        return ResponseEntity.notFound().build();
    }
}
