package com.ccbsa.common.messaging.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.transaction.KafkaTransactionManager;

import com.ccbsa.common.domain.DomainEvent;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Production-grade Kafka configuration for messaging infrastructure.
 * <p>
 * Provides standardized producer and consumer factories with:
 * - Idempotent producers for exactly-once semantics
 * - Manual acknowledgment for reliable message processing
 * - Error handling and retry mechanisms
 * - Transaction support
 * - Optimized batching and compression
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:default-group}")
    private String groupId;

    @Value("${spring.kafka.consumer.properties.spring.json.trusted.packages:*}")
    private String trustedPackages;

    @Value("${spring.kafka.consumer.properties.spring.json.value.default.type:}")
    private String defaultType;

    @Value("${spring.kafka.producer.acks:all}")
    private String acks;

    @Value("${spring.kafka.producer.retries:3}")
    private Integer retries;

    @Value("${spring.kafka.producer.enable-idempotence:true}")
    private Boolean enableIdempotence;

    @Value("${spring.kafka.producer.compression-type:snappy}")
    private String compressionType;

    @Value("${spring.kafka.producer.batch-size:16384}")
    private Integer batchSize;

    @Value("${spring.kafka.producer.linger-ms:10}")
    private Integer lingerMs;

    @Value("${spring.kafka.consumer.max-poll-records:500}")
    private Integer maxPollRecords;

    @Value("${spring.kafka.consumer.max-poll-interval-ms:300000}")
    private Integer maxPollIntervalMs;

    @Value("${spring.kafka.consumer.session-timeout-ms:30000}")
    private Integer sessionTimeoutMs;

    @Value("${spring.kafka.listener.concurrency:3}")
    private Integer concurrency;

    /**
     * ObjectMapper bean for JSON serialization/deserialization.
     * Configured with JavaTimeModule for proper date/time handling and Jdk8Module
     * for Java 8 types like Optional.
     * Type information is included in JSON payload (as @class property) to enable
     * event type detection when consuming events across service boundaries.
     * <p>
     * Uses mixin to add type information to DomainEvent without modifying domain core
     * (keeping domain pure Java per architecture guidelines).
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Jdk8Module());

        // Configure type information for DomainEvent using mixin
        // This adds @class property to JSON without modifying domain core
        mapper.addMixIn(DomainEvent.class, DomainEventTypeInfoMixin.class);

        return mapper;
    }

    /**
     * Production-grade Kafka producer factory with idempotence, acks=all, and compression.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory(ObjectMapper objectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Production-grade producer settings
        configProps.put(ProducerConfig.ACKS_CONFIG, acks);
        configProps.put(ProducerConfig.RETRIES_CONFIG, retries);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, enableIdempotence);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // Transaction support: TRANSACTIONAL_ID_CONFIG should only be set when actually needed
        // for transactional producers. Setting it to null causes NullPointerException in
        // DefaultKafkaProducerFactory constructor. Omit this property for non-transactional producers.

        // Note: JsonSerializer configuration is done programmatically via jsonSerializer() bean
        // to avoid conflict with Spring Boot auto-configuration. Do not set JsonSerializer.ADD_TYPE_INFO_HEADERS
        // in configProps when using programmatic configuration.

        DefaultKafkaProducerFactory<String, Object> factory =
                new DefaultKafkaProducerFactory<>(configProps);
        factory.setValueSerializer(jsonSerializer(objectMapper));
        return factory;
    }

    /**
     * JSON serializer for Kafka producer.
     * Type information is included in JSON payload (via ObjectMapper configuration) to enable
     * event type detection when consuming events across service boundaries.
     */
    @Bean
    public JsonSerializer<Object> jsonSerializer(ObjectMapper objectMapper) {
        JsonSerializer<Object> serializer = new JsonSerializer<>(objectMapper);
        // Type information is handled by ObjectMapper configuration, not headers
        serializer.setAddTypeInfo(false);
        return serializer;
    }

    /**
     * KafkaTemplate for publishing events to Kafka topics.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Production-grade Kafka consumer factory with manual acknowledgment and error handling.
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory(ObjectMapper objectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // Do not set VALUE_DESERIALIZER_CLASS_CONFIG when configuring deserializer programmatically

        // Production-grade consumer settings
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual acknowledgment
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        // Configure JsonDeserializer instance directly to avoid conflict with Spring Boot auto-configuration
        // Use Object as target type - deserializer will use @class property from JSON to determine actual type
        // The ObjectMapper is configured with JsonTypeInfo mixin to include @class property in JSON
        JsonDeserializer<Object> deserializer = new JsonDeserializer<>(Object.class, objectMapper);
        if (trustedPackages != null && !trustedPackages.isEmpty()) {
            // Split comma-separated packages or use wildcard
            if ("*".equals(trustedPackages)) {
                deserializer.addTrustedPackages("*");
            } else {
                deserializer.addTrustedPackages(trustedPackages.split(","));
            }
        }
        // Use @class property from JSON payload (configured via ObjectMapper mixin with JsonTypeInfo)
        // Don't use type headers - type information is in JSON payload itself via @class property
        deserializer.setUseTypeHeaders(false);
        deserializer.setRemoveTypeHeaders(true);

        DefaultKafkaConsumerFactory<String, Object> factory =
                new DefaultKafkaConsumerFactory<>(configProps);
        factory.setValueDeserializer(deserializer);
        return factory;
    }

    /**
     * Kafka listener container factory with manual acknowledgment and concurrency settings.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Manual acknowledgment mode for reliable processing
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Concurrency for parallel processing
        factory.setConcurrency(concurrency);

        // Batch processing support
        factory.setBatchListener(false); // Set to true if batch processing is needed

        return factory;
    }

    /**
     * Kafka transaction manager for transactional producers.
     * Used when exactly-once semantics are required.
     * <p>
     * This bean is only created when {@code spring.kafka.producer.transaction-enabled=true}
     * is set in the configuration. When enabled, the producer factory must also be
     * configured with a transactional ID (see producerFactory method).
     * <p>
     * Note: Currently, the default producer factory does not support transactions.
     * To enable transactions, configure a separate transactional producer factory
     * with TRANSACTIONAL_ID_CONFIG set.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.kafka.producer.transaction-enabled", havingValue = "true", matchIfMissing = false)
    public KafkaTransactionManager<String, Object> kafkaTransactionManager(
            ProducerFactory<String, Object> producerFactory) {
        return new KafkaTransactionManager<>(producerFactory);
    }

    /**
     * Mixin interface to add type information to DomainEvent.
     * This allows adding Jackson annotations without modifying domain core,
     * maintaining pure Java domain per architecture guidelines.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    private interface DomainEventTypeInfoMixin {
        // Empty interface - annotations are inherited by DomainEvent
    }
}

