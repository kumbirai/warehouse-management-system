package com.ccbsa.wms.user.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate configuration for User service. Configures timeouts, connection pooling, and connection settings.
 * <p>
 * Provides two RestTemplate beans:
 * 1. {@code loadBalancedRestTemplate} - For service-to-service calls via Eureka (e.g., tenant-service)
 * 2. {@code externalRestTemplate} - For external services NOT registered with Eureka (e.g., Keycloak)
 * <p>
 * Production-grade features: - Connection pooling for better performance - Configurable timeouts - Connection reuse - Maximum connections per route
 */
@Configuration
public class RestTemplateConfig {
    private static final int MAX_TOTAL_CONNECTIONS = 200;
    private static final int MAX_CONNECTIONS_PER_ROUTE = 50;
    private static final int CONNECTION_TIMEOUT_SECONDS = 10;  // Increased for better reliability
    private static final int READ_TIMEOUT_SECONDS = 25;  // Increased to 25s to match frontend timeout (30s) with buffer
    private static final int CONNECTION_REQUEST_TIMEOUT_SECONDS = 10;  // Increased for better reliability

    /**
     * Load-balanced RestTemplate for service-to-service calls via Eureka.
     * <p>
     * Use this for calls to services registered with Eureka (e.g., tenant-service).
     * The LoadBalancer will resolve service names through Eureka registry.
     * <p>
     * Example: {@code restTemplate.getForObject("http://tenant-service/api/v1/tenants/...", ...)}
     *
     * @param builder RestTemplateBuilder
     * @return Load-balanced RestTemplate for Eureka service discovery
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate(RestTemplateBuilder builder) {
        return createRestTemplateWithConnectionPool(builder);
    }

    /**
     * Creates a RestTemplate with connection pooling and timeouts.
     * This is a helper method to avoid code duplication.
     *
     * @param builder RestTemplateBuilder
     * @return RestTemplate with connection pool and timeouts
     */
    private RestTemplate createRestTemplateWithConnectionPool(RestTemplateBuilder builder) {
        // Create connection pool manager
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);

        // Configure request timeouts
        // Note: setConnectTimeout is deprecated in HttpClient 5 but still functional.
        // The replacement API is not yet stable, so we continue using this method.
        // This is a known limitation and will be updated when HttpClient 5 provides a stable replacement.
        @SuppressWarnings("deprecation") RequestConfig requestConfig =
                RequestConfig.custom().setConnectTimeout(Timeout.ofSeconds(CONNECTION_TIMEOUT_SECONDS)).setResponseTimeout(Timeout.ofSeconds(READ_TIMEOUT_SECONDS))
                        .setConnectionRequestTimeout(Timeout.ofSeconds(CONNECTION_REQUEST_TIMEOUT_SECONDS)).build();

        // Create HTTP client with connection pooling
        HttpClient httpClient =
                HttpClientBuilder.create().setConnectionManager(connectionManager).setDefaultRequestConfig(requestConfig).evictIdleConnections(Timeout.ofSeconds(30))
                        .evictExpiredConnections().build();

        // Create request factory with pooled HTTP client
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return builder.requestFactory(() -> requestFactory).build();
    }

    /**
     * Non-load-balanced RestTemplate for external services NOT registered with Eureka.
     * <p>
     * Use this for calls to external services like Keycloak, which are not registered
     * with Eureka and should be called directly (e.g., http://localhost:7080).
     * <p>
     * Example: {@code externalRestTemplate.getForObject("http://localhost:7080/realms/...", ...)}
     * <p>
     * <b>IMPORTANT:</b> This RestTemplate does NOT use LoadBalancer, so it will make
     * direct HTTP calls without service discovery.
     *
     * @param builder RestTemplateBuilder
     * @return Non-load-balanced RestTemplate for external services
     */
    @Bean(name = "externalRestTemplate")
    public RestTemplate externalRestTemplate(RestTemplateBuilder builder) {
        return createRestTemplateWithConnectionPool(builder);
    }

    /**
     * Default RestTemplate bean (alias for loadBalancedRestTemplate for backward compatibility).
     * <p>
     * This bean is provided for backward compatibility with existing code that injects
     * RestTemplate without specifying a qualifier. It delegates to loadBalancedRestTemplate.
     * <p>
     * <b>Note:</b> For new code, prefer explicitly injecting {@code loadBalancedRestTemplate}
     * or {@code externalRestTemplate} based on the use case.
     *
     * @param loadBalancedRestTemplate The load-balanced RestTemplate (injected by bean name)
     * @return Load-balanced RestTemplate (for backward compatibility)
     */
    @Bean
    @Primary
    public RestTemplate restTemplate(@Qualifier("loadBalancedRestTemplate") RestTemplate loadBalancedRestTemplate) {
        // Return the load-balanced RestTemplate as the default
        // This maintains backward compatibility with existing code
        return loadBalancedRestTemplate;
    }
}

