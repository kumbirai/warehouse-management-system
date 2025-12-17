package com.ccbsa.wms.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Retry configuration. Enables Spring Retry for handling transient failures.
 * <p>
 * Retry policies are configured via annotations on methods that need retry logic.
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // Retry configuration is done via @Retryable annotations
    // See AuthenticationServiceAdapter for usage examples
}

