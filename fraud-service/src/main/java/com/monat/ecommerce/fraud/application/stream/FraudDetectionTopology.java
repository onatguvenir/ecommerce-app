package com.monat.ecommerce.fraud.application.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monat.ecommerce.events.fraud.UserAccountSuspendedEvent;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.SlidingWindows;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
@Setter  // Allows setter injection in unit tests (TopologyTestDriver pattern)
public class FraudDetectionTopology {

    @Value("${app.fraud.payment-failed-topic:payment-events}")
    private String paymentEventsTopic;

    @Value("${app.fraud.user-suspended-topic:user-suspension-events}")
    private String userSuspendedTopic;

    @Value("${app.fraud.window-minutes:5}")
    private int windowMinutes;

    @Value("${app.fraud.max-failures:5}")
    private int maxFailures;

    private final ObjectMapper objectMapper;

    /** KafkaStreams JSON field names kept as constants to avoid string duplication. */
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_FAILURE_REASON = "failureReason";
    private static final String FIELD_ORDER_ID = "orderId";

    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder) {
        log.info("Building Fraud Detection Topology with Sliding Window...");

        // JSON Serdes
        JsonSerde<JsonNode> jsonNodeSerde = new JsonSerde<>(JsonNode.class, objectMapper);
        JsonSerde<UserAccountSuspendedEvent> suspendedEventSerde = new JsonSerde<>(UserAccountSuspendedEvent.class, objectMapper);

        KStream<String, JsonNode> paymentStream = streamsBuilder.stream(
                paymentEventsTopic,
                Consumed.with(Serdes.String(), jsonNodeSerde)
        );

        // Process only PaymentFailedEvent
        paymentStream
                .filter((key, value) -> isPaymentFailedEvent(value))
                .filter((key, value) -> hasUserId(value))
                // Change the key to userId for grouping
                .selectKey((key, value) -> getUserId(value))
                .groupByKey(Grouped.with(Serdes.String(), jsonNodeSerde))
                // Sliding window to detect 'maxFailures' within 'windowMinutes'
                .windowedBy(SlidingWindows.ofTimeDifferenceAndGrace(Duration.ofMinutes(windowMinutes), Duration.ofMinutes(1)))
                .count(Materialized.<String, Long, WindowStore<org.apache.kafka.common.utils.Bytes, byte[]>>as("fraud-count-store"))
                .toStream()
                // Filter users who exceeded the threshold
                .filter((windowedUserId, count) -> count >= maxFailures)
                // Distinct until changed or manual deduplication can be added here
                // For simplicity, we just transform to SuspendedEvent
                .mapValues((windowedUserId, count) -> {
                    String userId = windowedUserId.key();
                    log.warn("FRAUD DETECTED! User {} exceeded max payment failures ({} times in {} mins).", userId, count, windowMinutes);
                    return UserAccountSuspendedEvent.create(
                            userId, 
                            "Exceeded " + maxFailures + " failed payment attempts within " + windowMinutes + " minutes"
                    );
                })
                // Change key back to UserID (un-windowed)
                .selectKey((windowedUserId, event) -> windowedUserId.key())
                .to(userSuspendedTopic, Produced.with(Serdes.String(), suspendedEventSerde));
    }

    private boolean isPaymentFailedEvent(JsonNode value) {
        if (value == null) return false;
        return value.has(FIELD_FAILURE_REASON) && value.has(FIELD_ORDER_ID);
    }

    private boolean hasUserId(JsonNode value) {
        return value.has(FIELD_USER_ID) && !value.get(FIELD_USER_ID).isNull();
    }

    private String getUserId(JsonNode value) {
        return value.get(FIELD_USER_ID).asText();
    }
}
