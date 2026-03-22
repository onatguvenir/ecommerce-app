package com.monat.ecommerce.fraud.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Value("${app.fraud.payment-failed-topic:payment-events}")
    private String paymentEventsTopic;

    @Value("${app.fraud.user-suspended-topic:user-suspension-events}")
    private String userSuspensionEventsTopic;

    @Bean
    public NewTopic userSuspensionEventsTopic() {
        return TopicBuilder.name(userSuspensionEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
