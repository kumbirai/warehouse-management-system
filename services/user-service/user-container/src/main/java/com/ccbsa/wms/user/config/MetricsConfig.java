package com.ccbsa.wms.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Metrics configuration.
 * Configures Micrometer metrics for monitoring and observability.
 */
@Configuration
public class MetricsConfig {
    /**
     * Enables @Timed annotation support for method-level timing.
     *
     * @param registry Meter registry
     * @return Timed aspect
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}

