package com.ccbsa.common.messaging.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.ExponentialBackOff;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Kafka error handling configuration with dead letter queue support.
 * <p>
 * Provides: - Dead letter topic publishing for failed messages - Exponential backoff retry mechanism - Error handlers for listener containers - Configurable retry attempts and
 * intervals
 */
@Configuration
public class KafkaErrorHandlingConfig {
    private static final Logger logger = LoggerFactory.getLogger(KafkaErrorHandlingConfig.class);

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:default-group}")
    private String groupId;

    @Value("${spring.kafka.consumer.properties.spring.json.trusted.packages:*}")
    private String trustedPackages;

    @Value("${spring.kafka.consumer.properties.spring.json.value.default.type:}")
    private String defaultType;

    @Value("${spring.kafka.error-handling.max-retries:3}")
    private Integer maxRetries;

    @Value("${spring.kafka.error-handling.initial-interval:1000}")
    private Long initialInterval;

    @Value("${spring.kafka.error-handling.multiplier:2.0}")
    private Double multiplier;

    @Value("${spring.kafka.error-handling.max-interval:10000}")
    private Long maxInterval;

    @Value("${spring.kafka.error-handling.dead-letter-topic-suffix:.dlq}")
    private String deadLetterTopicSuffix;

    /**
     * Consumer factory for dead letter queue publishing. Uses same configuration as main consumer but with different group ID.
     * <p>
     * Explicitly uses kafkaObjectMapper to ensure type information is properly deserialized.
     */
    @Bean
    public ConsumerFactory<String, Object> dlqConsumerFactory(@Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, String.format("%s-dlq", groupId));
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // Do not set VALUE_DESERIALIZER_CLASS_CONFIG when configuring deserializer programmatically
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Configure JsonDeserializer instance directly to avoid conflict with Spring Boot auto-configuration
        JsonDeserializer<Object> deserializer = new JsonDeserializer<>(kafkaObjectMapper);
        if (trustedPackages != null && !trustedPackages.isEmpty()) {
            // Split comma-separated packages or use wildcard
            if ("*".equals(trustedPackages)) {
                deserializer.addTrustedPackages("*");
            } else {
                deserializer.addTrustedPackages(trustedPackages.split(","));
            }
        }
        deserializer.setUseTypeHeaders(false);
        deserializer.setRemoveTypeHeaders(true);

        DefaultKafkaConsumerFactory<String, Object> factory = new DefaultKafkaConsumerFactory<>(configProps);
        factory.setValueDeserializer(deserializer);
        return factory;
    }

    /**
     * Producer factory for dead letter queue publishing.
     * <p>
     * Explicitly uses kafkaObjectMapper to ensure type information is included in Kafka messages.
     */
    @Bean
    public org.springframework.kafka.core.ProducerFactory<String, Object> dlqProducerFactory(@Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonSerializer.class);

        // Use same producer config as main producer
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);

        // Note: JsonSerializer configuration is done programmatically to avoid conflict with
        // Spring Boot auto-configuration. Do not set JsonSerializer.ADD_TYPE_INFO_HEADERS
        // in configProps when using programmatic configuration.

        org.springframework.kafka.core.DefaultKafkaProducerFactory<String, Object> factory = new org.springframework.kafka.core.DefaultKafkaProducerFactory<>(configProps);
        org.springframework.kafka.support.serializer.JsonSerializer<Object> serializer = new org.springframework.kafka.support.serializer.JsonSerializer<>(kafkaObjectMapper);
        serializer.setAddTypeInfo(false);
        factory.setValueSerializer(serializer);
        return factory;
    }

    /**
     * Dead letter publishing recoverer. Publishes failed messages to dead letter topic with suffix.
     */
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<String, Object> kafkaTemplate) {
        return new DeadLetterPublishingRecoverer(kafkaTemplate, (record, ex) -> {
            String originalTopic = record.topic();
            String dlqTopic = originalTopic + deadLetterTopicSuffix;
            logger.warn("Publishing failed message to dead letter topic: {} -> {}", originalTopic, dlqTopic);
            return new org.apache.kafka.common.TopicPartition(dlqTopic, record.partition());
        });
    }

    /**
     * Exponential backoff configuration for retry mechanism.
     */
    @Bean
    public BackOff exponentialBackOff() {
        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(initialInterval);
        backOff.setMultiplier(multiplier);
        backOff.setMaxInterval(maxInterval);
        backOff.setMaxElapsedTime(maxInterval * maxRetries);
        return backOff;
    }

    /**
     * Common error handler with dead letter queue and exponential backoff.
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer deadLetterPublishingRecoverer, BackOff backOff) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetterPublishingRecoverer, backOff);

        // Skip retries for certain exceptions (e.g., deserialization errors)
        errorHandler.addNotRetryableExceptions(org.apache.kafka.common.errors.SerializationException.class,
                org.springframework.kafka.support.serializer.DeserializationException.class);

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            logger.warn("Retry attempt {} for message from topic {}: {}", deliveryAttempt, record.topic(), ex.getMessage());
        });

        return errorHandler;
    }

    /**
     * Enhanced listener container factory with error handling.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactoryWithErrorHandling(ConsumerFactory<String, Object> consumerFactory,
                                                                                                                  CommonErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);

        // Manual acknowledgment mode
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }
}

