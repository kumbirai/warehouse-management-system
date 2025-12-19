package com.ccbsa.wms.gateway.api.util;

import java.time.Duration;

import javax.net.ssl.SSLException;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.web.reactive.server.WebTestClient;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.netty.http.client.HttpClient;

/**
 * Configuration for WebTestClient instances.
 */
public class WebTestClientConfig {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final int DEFAULT_BUFFER_SIZE = 16 * 1024 * 1024; // 16MB

    /**
     * Create WebTestClient with default configuration.
     * Supports both HTTP and HTTPS, with SSL certificate validation disabled for self-signed certificates.
     *
     * @param baseUrl the gateway base URL
     * @return configured WebTestClient
     */
    public static WebTestClient createWebTestClient(String baseUrl) {
        boolean isHttps = baseUrl != null && baseUrl.startsWith("https://");

        if (isHttps) {
            try {
                // Configure SSL context to trust all certificates (for self-signed certs in dev/test)
                SslContext sslContext = SslContextBuilder
                        .forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();

                // Create HTTP client with SSL support for HTTPS URLs
                HttpClient httpClient = HttpClient.create()
                        .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));

                return WebTestClient.bindToServer(new ReactorClientHttpConnector(httpClient))
                        .baseUrl(baseUrl)
                        .responseTimeout(DEFAULT_TIMEOUT)
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(DEFAULT_BUFFER_SIZE))
                        .build();
            } catch (SSLException e) {
                // Fallback to default configuration if SSL setup fails
                return WebTestClient.bindToServer()
                        .baseUrl(baseUrl)
                        .responseTimeout(DEFAULT_TIMEOUT)
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(DEFAULT_BUFFER_SIZE))
                        .build();
            }
        } else {
            // For HTTP URLs, use plain HTTP client without SSL
            return WebTestClient.bindToServer()
                    .baseUrl(baseUrl)
                    .responseTimeout(DEFAULT_TIMEOUT)
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(DEFAULT_BUFFER_SIZE))
                    .build();
        }
    }

    /**
     * Create WebTestClient with custom timeout.
     * Supports both HTTP and HTTPS, with SSL certificate validation disabled for self-signed certificates.
     *
     * @param baseUrl the gateway base URL
     * @param timeout the timeout duration
     * @return configured WebTestClient
     */
    public static WebTestClient createWebTestClient(String baseUrl, Duration timeout) {
        boolean isHttps = baseUrl != null && baseUrl.startsWith("https://");

        if (isHttps) {
            try {
                // Configure SSL context to trust all certificates (for self-signed certs in dev/test)
                SslContext sslContext = SslContextBuilder
                        .forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();

                // Create HTTP client with SSL support for HTTPS URLs
                HttpClient httpClient = HttpClient.create()
                        .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));

                return WebTestClient.bindToServer(new ReactorClientHttpConnector(httpClient))
                        .baseUrl(baseUrl)
                        .responseTimeout(timeout)
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(DEFAULT_BUFFER_SIZE))
                        .build();
            } catch (SSLException e) {
                // Fallback to default configuration if SSL setup fails
                return WebTestClient.bindToServer()
                        .baseUrl(baseUrl)
                        .responseTimeout(timeout)
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(DEFAULT_BUFFER_SIZE))
                        .build();
            }
        } else {
            // For HTTP URLs, use plain HTTP client without SSL
            return WebTestClient.bindToServer()
                    .baseUrl(baseUrl)
                    .responseTimeout(timeout)
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(DEFAULT_BUFFER_SIZE))
                    .build();
        }
    }
}

