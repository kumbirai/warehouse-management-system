package com.ccbsa.wms.user.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.ExponentialBackOff;

import com.ccbsa.common.messaging.config.KafkaConfig;
import com.ccbsa.wms.common.dataaccess.config.MultiTenantDataAccessConfig;
import com.ccbsa.wms.common.security.ServiceSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * User Service Configuration
 * <p>
 * Imports common security configuration for JWT validation and tenant context.
 * Imports common data access configuration for multi-tenant schema resolution.
 * Imports Kafka configuration for messaging infrastructure (provides kafkaObjectMapper bean).
 * <p>
 * The {@link MultiTenantDataAccessConfig} provides the {@link com.ccbsa.wms.common.dataaccess.TenantSchemaResolver}
 * bean which implements schema-per-tenant strategy for multi-tenant isolation.
 * <p>
 * The {@link KafkaConfig} provides the {@code kafkaObjectMapper} bean used for Kafka message
 * serialization/deserialization with type information.
 */
@Configuration
@Import( {ServiceSecurityConfig.class, MultiTenantDataAccessConfig.class, KafkaConfig.class})
public class UserServiceConfiguration {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.max-poll-records:500}")
    private Integer maxPollRecords;

    @Value("${spring.kafka.consumer.max-poll-interval-ms:300000}")
    private Integer maxPollIntervalMs;

    @Value("${spring.kafka.consumer.session-timeout-ms:30000}")
    private Integer sessionTimeoutMs;

    @Value("${spring.kafka.listener.concurrency:3}")
    private Integer concurrency;

    @Value("${spring.kafka.error-handling.max-retries:3}")
    private Integer maxRetries;

    @Value("${spring.kafka.error-handling.initial-interval:1000}")
    private Long initialInterval;

    @Value("${spring.kafka.error-handling.multiplier:2.0}")
    private Double multiplier;

    @Value("${spring.kafka.error-handling.max-interval:10000}")
    private Long maxInterval;

    /**
     * Custom consumer factory for external events (tenant events).
     * <p>
     * Uses typed deserialization with @class property from JSON payload.
     * The ObjectMapper is configured with JsonTypeInfo to include @class property,
     * allowing proper typed deserialization across service boundaries.
     * <p>
     * Uses ErrorHandlingDeserializer to gracefully handle deserialization errors.
     * <p>
     * Explicitly uses kafkaObjectMapper to ensure type information is properly deserialized.
     *
     * @param kafkaObjectMapper Kafka ObjectMapper for JSON deserialization (configured with JsonTypeInfo)
     * @return Consumer factory configured for typed deserialization
     */
    @Bean("externalEventConsumerFactory")
    public ConsumerFactory<String, Object> externalEventConsumerFactory(
            @Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "user-service");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        // Configure JsonDeserializer to properly handle polymorphic deserialization
        // When deserializing to Object.class, Jackson uses the ObjectMapper's type information
        // configuration (mixin with @class property in As.PROPERTY format)
        // The ObjectMapper is configured with mixins for DomainEvent and Object.class to use PROPERTY format
        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(Object.class, kafkaObjectMapper);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeHeaders(false);
        jsonDeserializer.setRemoveTypeHeaders(true);

        ErrorHandlingDeserializer<Object> errorHandlingDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);

        DefaultKafkaConsumerFactory<String, Object> factory =
                new DefaultKafkaConsumerFactory<>(configProps);
        factory.setValueDeserializer(errorHandlingDeserializer);
        return factory;
    }

    /**
     * Custom listener container factory for external events (tenant events).
     * <p>
     * Uses the externalEventConsumerFactory to properly deserialize external events as Map.
     * <p>
     * Includes error handling to skip deserialization errors and continue processing other messages.
     *
     * @param externalEventConsumerFactory Consumer factory for external events
     * @return Listener container factory
     */
    @Bean("externalEventKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> externalEventKafkaListenerContainerFactory(
            ConsumerFactory<String, Object> externalEventConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(externalEventConsumerFactory);

        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(concurrency);

        BackOff backOff = createExponentialBackOff();
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(backOff);
        errorHandler.addNotRetryableExceptions(
                org.apache.kafka.common.errors.SerializationException.class,
                org.springframework.kafka.support.serializer.DeserializationException.class
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    private BackOff createExponentialBackOff() {
        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(initialInterval);
        backOff.setMultiplier(multiplier);
        backOff.setMaxInterval(maxInterval);
        backOff.setMaxElapsedTime(maxInterval * maxRetries);
        return backOff;
    }

    /**
     * Custom consumer factory for cache invalidation events (user events).
     * <p>
     * Uses typed deserialization with @class property from JSON payload.
     * The ObjectMapper is configured with JsonTypeInfo to include @class property,
     * allowing proper typed deserialization of user domain events.
     * <p>
     * Uses ErrorHandlingDeserializer to gracefully handle deserialization errors.
     * <p>
     * Explicitly uses kafkaObjectMapper to ensure type information is properly deserialized.
     *
     * @param kafkaObjectMapper Kafka ObjectMapper for JSON deserialization (configured with JsonTypeInfo)
     * @return Consumer factory configured for cache invalidation events
     */
    @Bean("cacheInvalidationConsumerFactory")
    public ConsumerFactory<String, Object> cacheInvalidationConsumerFactory(
            @Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "user-service-cache-invalidation");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        // Configure JsonDeserializer to properly handle polymorphic deserialization
        // When deserializing to Object.class, Jackson uses the ObjectMapper's type information
        // configuration (mixin with @class property in As.PROPERTY format)
        // The ObjectMapper is configured with mixins for DomainEvent and Object.class to use PROPERTY format
        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(Object.class, kafkaObjectMapper);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeHeaders(false);
        jsonDeserializer.setRemoveTypeHeaders(true);

        ErrorHandlingDeserializer<Object> errorHandlingDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);

        DefaultKafkaConsumerFactory<String, Object> factory =
                new DefaultKafkaConsumerFactory<>(configProps);
        factory.setValueDeserializer(errorHandlingDeserializer);
        return factory;
    }

    /**
     * Custom listener container factory for cache invalidation events.
     * <p>
     * Uses the cacheInvalidationConsumerFactory to properly deserialize user events
     * with error handling support.
     * <p>
     * Includes error handling to skip deserialization errors and continue processing other messages.
     *
     * @param cacheInvalidationConsumerFactory Consumer factory for cache invalidation events
     * @return Listener container factory
     */
    @Bean("cacheInvalidationKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> cacheInvalidationKafkaListenerContainerFactory(
            ConsumerFactory<String, Object> cacheInvalidationConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cacheInvalidationConsumerFactory);

        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(concurrency);

        BackOff backOff = createExponentialBackOff();
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(backOff);
        errorHandler.addNotRetryableExceptions(
                org.apache.kafka.common.errors.SerializationException.class,
                org.springframework.kafka.support.serializer.DeserializationException.class
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}

