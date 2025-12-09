package com.ccbsa.wms.gateway.api.util;

import java.time.Duration;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.netty.http.client.HttpClient;

/**
 * Configuration utility for WebTestClient with SSL support for testing against HTTPS gateway.
 */
public final class WebTestClientConfig {

    private static final int MAX_IN_MEMORY_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private WebTestClientConfig() {
        // Utility class
    }

    /**
     * Creates a WebTestClient configured for HTTPS with relaxed SSL validation.
     *
     * @param baseUrl The base URL of the gateway (e.g., https://localhost:8080/api/v1)
     * @return Configured WebTestClient instance
     */
    public static WebTestClient createWebTestClient(String baseUrl) {
        try {
            // Create SSL context with relaxed validation for self-signed certificates
            SslContext sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();

            // Create HTTP client with SSL support
            HttpClient httpClient = HttpClient.create()
                    .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext))
                    .responseTimeout(DEFAULT_TIMEOUT);

            // Configure exchange strategies for large responses
            ExchangeStrategies strategies = ExchangeStrategies.builder()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
                    .build();

            return WebTestClient
                    .bindToServer()
                    .baseUrl(baseUrl)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .exchangeStrategies(strategies)
                    .responseTimeout(DEFAULT_TIMEOUT)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create WebTestClient with SSL support", e);
        }
    }
}

