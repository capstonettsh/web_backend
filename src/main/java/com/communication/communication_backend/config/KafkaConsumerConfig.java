package com.communication.communication_backend.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Set the bootstrap servers (ensure this property is correctly set)
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Set the key and value deserializers
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Set the consumer group ID
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "humeai");

        // (Optional) Set auto-offset reset to earliest (so the consumer reads from the beginning)
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Add any additional properties you need
        // For example:
        // props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        // props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        // props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000);

        return new DefaultKafkaConsumerFactory<>(props);
    }
}
