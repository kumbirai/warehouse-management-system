package com.ccbsa.wms.gateway.config;

import java.util.List;
import java.util.Locale;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import lombok.extern.slf4j.Slf4j;
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
 * <p>
 * <b>CRITICAL:</b> This configuration properly delegates service ID extraction
 * to the default supplier to ensure the service ID is correctly extracted from
 * the lb:// URI (e.g., lb://user-service extracts "user-service").
 * The service ID is extracted from the LoadBalancerRequestContext by the default
 * supplier, which is set by Spring Cloud Gateway's LoadBalancerClientFilter.
 * <p>
 * <b>IMPORTANT:</b> The wrapper delegates all method calls to the default supplier
 * to preserve the LoadBalancerRequestContext and ensure correct service ID extraction.
 */
@Slf4j
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

            // Get the default supplier with health checks enabled
            // This supplier properly extracts service ID from LoadBalancerRequestContext
            // The service ID is set by ReactiveLoadBalancerClientFilter from the lb:// URI
            // 
            // CRITICAL: The default supplier's getServiceId() method accesses the service ID
            // from the LoadBalancerRequestContext, which is set by ReactiveLoadBalancerClientFilter
            // from the route's URI (lb://service-name). The service ID is extracted from the
            // URI's host part (e.g., "user-service" from "lb://user-service").
            // 
            // IMPORTANT: We wrap the supplier to add case-insensitive lookup while preserving
            // the LoadBalancerRequestContext. The wrapper delegates ALL method calls to the
            // default supplier to ensure correct service ID extraction.
            ServiceInstanceListSupplier defaultSupplier = ServiceInstanceListSupplier.builder().withDiscoveryClient().withCaching().withHealthChecks().build(context);

            // Wrap it to add case-insensitive lookup while preserving service ID context
            // The decorator delegates to the default supplier to preserve LoadBalancerRequestContext
            return new CaseInsensitiveServiceInstanceListSupplierDecorator(defaultSupplier, discoveryClient);
        }
    }

    /**
     * Decorator for ServiceInstanceListSupplier that adds case-insensitive service name resolution.
     * <p>
     * This decorator wraps the default supplier and adds case-insensitive lookup while
     * preserving the LoadBalancerRequestContext to ensure correct service ID extraction.
     */
    @Slf4j
    static class CaseInsensitiveServiceInstanceListSupplierDecorator implements ServiceInstanceListSupplier {
        private final ServiceInstanceListSupplier delegate;
        private final DiscoveryClient discoveryClient;

        CaseInsensitiveServiceInstanceListSupplierDecorator(ServiceInstanceListSupplier delegate, DiscoveryClient discoveryClient) {
            this.delegate = delegate;
            this.discoveryClient = discoveryClient;
        }

        @Override
        public Flux<List<ServiceInstance>> get() {
            // CRITICAL: Get service ID from delegate supplier
            // The service ID is extracted from the lb:// URI by ReactiveLoadBalancerClientFilter
            // and stored in LoadBalancerRequestContext
            // We MUST call getServiceId() on the delegate supplier to get it from context
            String serviceId = delegate.getServiceId();

            log.debug("LoadBalancer resolving service: serviceId={}", serviceId);

            // Validate service ID is not null or empty
            if (serviceId == null || serviceId.trim().isEmpty()) {
                log.error("Service ID is null or empty. Route URI must use lb://service-name format.");
                return Flux.error(new IllegalStateException("Service ID cannot be null or empty. Ensure the route URI uses lb://service-name format."));
            }

            // Validate service ID is not "localhost" (indicates incorrect extraction)
            // This happens when the service ID is not correctly extracted from the lb:// URI
            // The LoadBalancerRequestContext should contain the service name from lb://service-name
            if ("localhost".equalsIgnoreCase(serviceId)) {
                log.error("Service ID extracted as 'localhost' instead of service name. " + "This indicates the service ID was not correctly extracted from the lb:// URI. "
                        + "The LoadBalancerRequestContext may not be properly set by ReactiveLoadBalancerClientFilter.");
                return Flux.error(new IllegalStateException(
                        "Service ID incorrectly extracted as 'localhost'. " + "Ensure route URI uses lb://service-name format (e.g., lb://user-service). "
                                + "The service ID should be extracted from the URI by ReactiveLoadBalancerClientFilter."));
            }

            // Skip case-insensitive lookup if service ID is already uppercase
            String uppercaseServiceId = serviceId.toUpperCase(Locale.ROOT);
            if (serviceId.equals(uppercaseServiceId)) {
                log.debug("Service ID is already uppercase, using delegate supplier: serviceId={}", serviceId);
                return delegate.get();
            }

            log.debug("Trying case-insensitive lookup: original={}, uppercase={}", serviceId, uppercaseServiceId);

            // CRITICAL: Eureka stores service names in uppercase (e.g., USER-SERVICE),
            // but gateway routes use lowercase (e.g., user-service).
            // We need to try both to handle this mismatch.
            // 
            // Strategy: Try both service IDs directly via DiscoveryClient first,
            // then fallback to delegate supplier if needed.
            // This ensures we find services regardless of how they're registered.
            // 
            // IMPORTANT: We try uppercase first since Eureka typically stores services in uppercase,
            // then try lowercase, then fallback to delegate supplier.
            return Flux.defer(() -> {
                // Try uppercase first (Eureka's typical format)
                log.debug("Trying uppercase serviceId={} first", uppercaseServiceId);
                try {
                    List<ServiceInstance> uppercaseInstances = discoveryClient.getInstances(uppercaseServiceId);
                    if (uppercaseInstances != null && !uppercaseInstances.isEmpty()) {
                        log.debug("Found {} instance(s) for uppercase serviceId={}", uppercaseInstances.size(), uppercaseServiceId);
                        return Flux.just(uppercaseInstances);
                    }
                } catch (Exception e) {
                    log.debug("Error querying uppercase serviceId={}: {}", uppercaseServiceId, e.getMessage());
                }

                // Try lowercase
                log.debug("Trying lowercase serviceId={}", serviceId);
                try {
                    List<ServiceInstance> lowercaseInstances = discoveryClient.getInstances(serviceId);
                    if (lowercaseInstances != null && !lowercaseInstances.isEmpty()) {
                        log.debug("Found {} instance(s) for lowercase serviceId={}", lowercaseInstances.size(), serviceId);
                        return Flux.just(lowercaseInstances);
                    }
                } catch (Exception e) {
                    log.debug("Error querying lowercase serviceId={}: {}", serviceId, e.getMessage());
                }

                // Fallback to delegate supplier (uses LoadBalancerRequestContext and caching)
                log.debug("Both direct queries failed, trying delegate supplier for serviceId={}", serviceId);
                return delegate.get();
            }).switchIfEmpty(
                    // If all attempts failed, log available services for debugging
                    Flux.defer(() -> {
                        try {
                            List<String> availableServices = discoveryClient.getServices();
                            log.warn("No service instances found for serviceId={} (tried '{}', '{}', and delegate supplier). " + "Available services in Eureka: {}", serviceId,
                                    uppercaseServiceId, serviceId, availableServices);
                        } catch (Exception e) {
                            log.error("Error getting available services: {}", e.getMessage());
                        }
                        return Flux.empty();
                    })).onErrorResume(error -> {
                log.error("Error in LoadBalancer service resolution for serviceId={}: {}", serviceId, error.getMessage(), error);
                // On error, try to get available services for debugging
                try {
                    List<String> availableServices = discoveryClient.getServices();
                    log.warn("Available services in Eureka: {}", availableServices);
                } catch (Exception e) {
                    log.error("Error getting available services: {}", e.getMessage());
                }
                return Flux.error(error);
            });
        }

        @Override
        public String getServiceId() {
            // CRITICAL: Delegate to default supplier to get service ID from request context
            // This extracts the service name from lb://service-name URI
            // The service ID is set by ReactiveLoadBalancerClientFilter in LoadBalancerRequestContext
            // We MUST delegate to preserve the context and ensure correct extraction
            String serviceId = delegate.getServiceId();
            log.debug("Service ID extracted from LoadBalancerRequestContext: serviceId={}", serviceId);

            // CRITICAL: Never return null to prevent NullPointerException in CachingServiceInstanceListSupplier
            // The cache uses getServiceId() as the cache key, and null keys cause NPE
            // Return empty string if serviceId is null (during initialization/startup)
            if (serviceId == null) {
                log.warn("Service ID is null from delegate supplier. This may occur during initialization. Returning empty string to prevent cache NPE.");
                return "";
            }

            return serviceId;
        }
    }
}

