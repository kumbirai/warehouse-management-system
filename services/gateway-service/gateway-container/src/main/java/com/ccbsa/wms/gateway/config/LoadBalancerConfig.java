package com.ccbsa.wms.gateway.config;

import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import reactor.core.publisher.Flux;

/**
 * LoadBalancer configuration to handle case-insensitive service name resolution.
 * <p>
 * Eureka stores service names in uppercase (e.g., USER-SERVICE), but the gateway
 * routes use lowercase names (e.g., user-service). This configuration ensures
 * that service lookups work by trying both the original service name and its
 * uppercase variant when querying Eureka.
 * <p>
 * This configuration applies to all services via the default configuration.
 */
@Configuration
@LoadBalancerClients(defaultConfiguration = LoadBalancerConfig.CaseInsensitiveServiceInstanceListSupplierConfiguration.class)
public class LoadBalancerConfig {

    /**
     * Default configuration for all LoadBalancer clients to handle case-insensitive
     * service name lookups.
     */
    @Configuration
    static class CaseInsensitiveServiceInstanceListSupplierConfiguration {

        @Bean
        @Primary
        public ServiceInstanceListSupplier caseInsensitiveServiceInstanceListSupplier(ConfigurableApplicationContext context, DiscoveryClient discoveryClient) {
            // Get the default supplier
            ServiceInstanceListSupplier defaultSupplier = ServiceInstanceListSupplier.builder().withDiscoveryClient().withCaching().build(context);

            // Wrap it to add case-insensitive lookup
            return new ServiceInstanceListSupplier() {
                @Override
                public Flux<List<ServiceInstance>> get() {
                    String serviceId = getServiceId();
                    String uppercaseServiceId = serviceId.toUpperCase();

                    // If already uppercase, use default supplier directly
                    if (serviceId.equals(uppercaseServiceId)) {
                        return defaultSupplier.get();
                    }

                    // Try with original service ID first
                    return defaultSupplier.get().switchIfEmpty(
                            // If not found, try with uppercase service ID using DiscoveryClient
                            Flux.defer(() -> {
                                List<ServiceInstance> instances = discoveryClient.getInstances(uppercaseServiceId);
                                return Flux.just(instances);
                            }));
                }

                @Override
                public String getServiceId() {
                    return defaultSupplier.getServiceId();
                }
            };
        }
    }
}

