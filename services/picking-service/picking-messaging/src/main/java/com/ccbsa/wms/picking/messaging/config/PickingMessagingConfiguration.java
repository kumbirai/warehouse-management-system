package com.ccbsa.wms.picking.messaging.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.ccbsa.common.messaging.config.KafkaConfig;

/**
 * Messaging Configuration: PickingMessagingConfiguration
 * <p>
 * Configures Kafka messaging for picking service.
 */
@Configuration
@Import(KafkaConfig.class)
public class PickingMessagingConfiguration {
    // Kafka configuration is imported from KafkaConfig
    // Consumer factories are configured with @Qualifier("kafkaObjectMapper")
}
