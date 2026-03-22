package com.monat.ecommerce.fraud.application.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.monat.ecommerce.events.fraud.UserAccountSuspendedEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for FraudDetectionTopology using TopologyTestDriver.
 *
 * Theory (Educational Note):
 * TopologyTestDriver is an in-memory, synchronous test harness provided by
 * Apache Kafka. It allows testing Kafka Streams topologies WITHOUT any real
 * Kafka cluster. Time is fully controlled programmatically, making it the
 * recommended approach for topology unit testing — fast, deterministic, and
 * no external infrastructure required.
 */
@DisplayName("FraudDetectionTopology Tests")
class FraudDetectionTopologyTest {

    private static final String INPUT_TOPIC = "payment.failed";
    private static final String OUTPUT_TOPIC = "user-suspension-events";
    private static final int MAX_FAILURES = 5;
    private static final int WINDOW_MINUTES = 5;

    private TopologyTestDriver testDriver;
    private TestInputTopic<String, JsonNode> inputTopic;
    private TestOutputTopic<String, UserAccountSuspendedEvent> outputTopic;
    private ObjectMapper objectMapper;
    Path stateDir;

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        FraudDetectionTopology topology = new FraudDetectionTopology(objectMapper);
        topology.setPaymentEventsTopic(INPUT_TOPIC);
        topology.setUserSuspendedTopic(OUTPUT_TOPIC);
        topology.setWindowMinutes(WINDOW_MINUTES);
        topology.setMaxFailures(MAX_FAILURES);

        StreamsBuilder builder = new StreamsBuilder();
        topology.buildPipeline(builder);

        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "fraud-detection-test");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        stateDir = Files.createDirectories(Paths.get("target", "kafka-streams-tests", UUID.randomUUID().toString()));
        // Keep Kafka Streams state isolated per test run and out of the shared OS temp dir.
        config.put(StreamsConfig.STATE_DIR_CONFIG, stateDir.toAbsolutePath().toString());

        testDriver = new TopologyTestDriver(builder.build(), config);

        inputTopic = testDriver.createInputTopic(
                INPUT_TOPIC,
                new StringSerializer(),
                new JsonSerializer<>(objectMapper)
        );

        outputTopic = testDriver.createOutputTopic(
                OUTPUT_TOPIC,
                new StringDeserializer(),
                new JsonDeserializer<>(UserAccountSuspendedEvent.class, objectMapper)
        );
    }

    @AfterEach
    void tearDown() {
        testDriver.close();
    }

    @Test
    @DisplayName("Should emit suspension event when user exceeds max failures within window")
    void shouldDetectFraudWhenMaxFailuresExceeded() throws Exception {
        // Given: A user with 6 consecutive failed payments in 3 minutes
        String userId = "user-42";
        for (int i = 0; i < 6; i++) {
            inputTopic.pipeInput("key-" + i, buildFailedPaymentEvent(userId, "order-" + i));
        }

        // When: Read the output topic
        var outputs = outputTopic.readValuesToList();

        // Then: At least one suspension event should be emitted for the user
        assertThat(outputs).isNotEmpty();
        assertThat(outputs).allMatch(e -> e.userId().equals(userId));
    }

    @Test
    @DisplayName("Should NOT emit suspension event when failures are below threshold")
    void shouldNotDetectFraudWhenBelowThreshold() throws Exception {
        // Given: A user with only 3 failed payments (below MAX_FAILURES=5)
        String userId = "user-benign";
        for (int i = 0; i < 3; i++) {
            inputTopic.pipeInput("key-" + i, buildFailedPaymentEvent(userId, "order-" + i));
        }

        // When: Read the output topic
        var outputs = outputTopic.readValuesToList();

        // Then: No suspension events
        assertThat(outputs).isEmpty();
    }

    @Test
    @DisplayName("Should NOT process events without userId field")
    void shouldSkipEventsWithMissingUserId() throws Exception {
        // Given: Event with no userId
        ObjectNode event = objectMapper.createObjectNode();
        event.put("orderId", "order-99");
        event.put("failureReason", "Card declined");

        inputTopic.pipeInput("key-1", (JsonNode) event);

        var outputs = outputTopic.readValuesToList();
        assertThat(outputs).isEmpty();
    }

    private JsonNode buildFailedPaymentEvent(String userId, String orderId) throws Exception {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("paymentId", "pay-" + orderId);
        node.put("orderId", orderId);
        node.put("userId", userId);
        node.put("amount", "100.00");
        node.put("currency", "USD");
        node.put("failureReason", "Card declined");
        return node;
    }
}
