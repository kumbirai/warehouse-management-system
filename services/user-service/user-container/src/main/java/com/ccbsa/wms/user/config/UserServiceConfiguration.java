package com.ccbsa.wms.user.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
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

import com.ccbsa.wms.common.dataaccess.config.MultiTenantDataAccessConfig;
import com.ccbsa.wms.common.security.ServiceSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * User Service Configuration
 * <p>
 * Imports common security configuration for JWT validation and tenant context.
 * Imports common data access configuration for multi-tenant schema resolution.
 * <p>
 * The {@link MultiTenantDataAccessConfig} provides the {@link com.ccbsa.wms.common.dataaccess.TenantSchemaResolver}
 * bean which implements schema-per-tenant strategy for multi-tenant isolation.
 */
@Configuration
@Import( {ServiceSecurityConfig.class, MultiTenantDataAccessConfig.class})
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
     *
     * @param objectMapper ObjectMapper for JSON deserialization (configured with JsonTypeInfo)
     * @return Consumer factory configured for typed deserialization
     */
    @Bean("externalEventConsumerFactory")
    public ConsumerFactory<String, Object> externalEventConsumerFactory(ObjectMapper objectMapper) {
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

        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(Object.class, objectMapper);
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
}

