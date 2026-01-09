package com.ccbsa.wms.picking.config;

import java.util.List;
import java.util.Locale;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * LoadBalancer configuration to handle case-insensitive service name resolution.
 * <p>
 * Eureka stores service names in uppercase (e.g., STOCK-MANAGEMENT-SERVICE), but services
 * use lowercase names in URLs (e.g., http://stock-management-service). This configuration
 * ensures that service lookups work by trying both the original service name and
 * its uppercase variant when querying Eureka.
 * <p>
 * This configuration applies to all LoadBalancer clients in picking-service via the
 * default configuration.
 * <p>
 * <b>CRITICAL:</b> For RestTemplate with @LoadBalanced, the service ID is extracted
 * from the URL hostname (e.g., "stock-management-service" from "http://stock-management-service/api/...").
 * The LoadBalancerRequestContext contains the service ID extracted from the URL.
 * <p>
 * <b>IMPORTANT:</b> The decorator tries both lowercase and uppercase service IDs
 * to handle Eureka's uppercase service registration.
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
        public ServiceInstanceListSupplier caseInsensitiveServiceInstanceListSupplier(ConfigurableApplicationContext context, DiscoveryClient discoveryClient) {

            // Get the default supplier for blocking RestTemplate
            // CRITICAL: Use withBlockingDiscoveryClient() for blocking RestTemplate
            // (not withDiscoveryClient() which tries to use ReactiveDiscoveryClient)
            // NOTE: withHealthChecks() requires WebClient (reactive) and is not available
            // for blocking RestTemplate, so we omit it here
            // NOTE: withRetryAwareness() is automatically added by Spring Cloud LoadBalancer
            // when using withBlockingDiscoveryClient(), which creates retryAwareDiscoveryClientServiceInstanceListSupplier
            // This supplier properly extracts service ID from LoadBalancerRequestContext
            // For RestTemplate, the service ID is extracted from the URL hostname
            // (e.g., "stock-management-service" from "http://stock-management-service/api/...")
            ServiceInstanceListSupplier defaultSupplier = ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient().withCaching().build(context);

            // Wrap it to add case-insensitive lookup
            // The defaultSupplier may already be retry-aware (retryAwareDiscoveryClientServiceInstanceListSupplier)
            // We wrap it to add case-insensitive lookup while preserving retry awareness
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
            // For RestTemplate, the service ID is extracted from the URL hostname
            // (e.g., "stock-management-service" from "http://stock-management-service/api/...")
            String serviceId = delegate.getServiceId();

            log.debug("LoadBalancer resolving service: serviceId={}", serviceId);

            // Validate service ID is not null or empty
            // NOTE: During startup/initialization, serviceId may be null if called
            // outside of a request context. In this case, delegate to the default supplier.
            if (serviceId == null || serviceId.trim().isEmpty()) {
                log.debug("Service ID is null or empty during initialization, delegating to default supplier");
                return delegate.get();
            }

            // Handle "localhost" service ID - this indicates incorrect extraction
            // When service ID is "localhost", we can't determine the actual service name
            // from the context. In this case, we delegate to the default supplier which
            // may have access to the original URL through LoadBalancerRequestContext.
            // The default supplier should handle this case.
            if ("localhost".equalsIgnoreCase(serviceId)) {
                log.warn("Service ID extracted as 'localhost' instead of service name. " + "This may indicate the service ID was not correctly extracted from the URL. "
                        + "Delegating to default supplier which may have access to the original URL.");
                return delegate.get();
            }

            // Skip case-insensitive lookup if service ID is already uppercase
            String uppercaseServiceId = serviceId.toUpperCase(Locale.ROOT);
            if (serviceId.equals(uppercaseServiceId)) {
                log.debug("Service ID is already uppercase, using delegate supplier: serviceId={}", serviceId);
                return delegate.get();
            }

            log.debug("Trying case-insensitive lookup: original={}, uppercase={}", serviceId, uppercaseServiceId);

            // CRITICAL: Eureka stores service names in uppercase (e.g., STOCK-MANAGEMENT-SERVICE),
            // but services use lowercase in URLs (e.g., http://stock-management-service).
            // We try uppercase first since that's Eureka's typical format.
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

                // Fallback to delegate supplier (uses LoadBalancerRequestContext)
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
            // For RestTemplate, the service ID is extracted from the URL hostname
            // (e.g., "stock-management-service" from "http://stock-management-service/api/...")
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
