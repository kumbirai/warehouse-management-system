package com.ccbsa.wms.user.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate configuration for BFF service.
 * Configures timeouts, connection pooling, and connection settings for Keycloak API calls.
 * <p>
 * Production-grade features:
 * - Connection pooling for better performance
 * - Configurable timeouts
 * - Connection reuse
 * - Maximum connections per route
 */
@Configuration
public class RestTemplateConfig {
    private static final int MAX_TOTAL_CONNECTIONS = 200;
    private static final int MAX_CONNECTIONS_PER_ROUTE = 50;
    private static final int CONNECTION_TIMEOUT_SECONDS = 10;  // Increased for better reliability
    private static final int READ_TIMEOUT_SECONDS = 25;  // Increased to 25s to match frontend timeout (30s) with buffer
    private static final int CONNECTION_REQUEST_TIMEOUT_SECONDS = 10;  // Increased for better reliability

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Create connection pool manager
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);

        // Configure request timeouts
        // Note: setConnectTimeout is deprecated in HttpClient 5 but still functional.
        // The replacement API is not yet stable, so we continue using this method.
        // This is a known limitation and will be updated when HttpClient 5 provides a stable replacement.
        @SuppressWarnings("deprecation")
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
                .setResponseTimeout(Timeout.ofSeconds(READ_TIMEOUT_SECONDS))
                .setConnectionRequestTimeout(Timeout.ofSeconds(CONNECTION_REQUEST_TIMEOUT_SECONDS))
                .build();

        // Create HTTP client with connection pooling
        HttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictIdleConnections(Timeout.ofSeconds(30))
                .evictExpiredConnections()
                .build();

        // Create request factory with pooled HTTP client
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return builder.requestFactory(() -> requestFactory)
                .build();
    }
}

