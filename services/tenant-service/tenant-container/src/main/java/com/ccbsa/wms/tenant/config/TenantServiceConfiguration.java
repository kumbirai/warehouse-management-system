package com.ccbsa.wms.tenant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.ccbsa.common.messaging.config.KafkaConfig;
import com.ccbsa.wms.common.security.ServiceSecurityConfig;

/**
 * Tenant Service Configuration
 * <p>
 * Imports common security configuration for JWT validation and tenant context.
 * Imports common Kafka configuration to ensure events include @class field for type detection.
 */
@Configuration
@Import( {ServiceSecurityConfig.class, KafkaConfig.class})
public class TenantServiceConfiguration {
    // Service-specific configuration can be added here
}

