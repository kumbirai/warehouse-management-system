package com.ccbsa.wms.common.security;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Service Account Authentication Configuration
 * <p>
 * Production-grade configuration for service-to-service authentication using
 * service account tokens. This configuration should be imported by services
 * that need to make authenticated inter-service REST calls.
 * <p>
 * This configuration provides:
 * - {@link ServiceAccountTokenProvider} - Manages service account JWT tokens with caching
 * - {@link ServiceAccountAuthenticationInterceptor} - Auto-injects tokens in REST calls
 * - Configured {@link RestTemplate} with connection pooling and authentication
 * <p>
 * Usage in service container configuration:
 * <pre>
 * &#64;Configuration
 * &#64;Import(ServiceAccountAuthenticationConfig.class)
 * public class MyServiceConfiguration {
 *     // Service-specific configuration
 * }
 * </pre>
 * <p>
 * The configured RestTemplate will automatically:
 * 1. Forward Authorization header from HTTP request context (when available)
 * 2. Use service account token for event-driven calls (no HTTP context)
 * 3. Forward X-Tenant-Id header for multi-tenant isolation
 * 4. Handle token refresh automatically
 * <p>
 * <b>Important:</b> Ensure the following properties are configured in application.yml:
 * <pre>
 * keycloak:
 *   service-account:
 *     token-endpoint: http://localhost:7080/realms/wms-realm/protocol/openid-connect/token
 *     client-id: wms-service-account
 *     client-secret: ${SERVICE_ACCOUNT_CLIENT_SECRET}
 * </pre>
 *
 * @see ServiceAccountTokenProvider
 * @see ServiceAccountAuthenticationInterceptor
 */
@Configuration
@Slf4j
public class ServiceAccountAuthenticationConfig {

    private static final int MAX_TOTAL_CONNECTIONS = 200;
    private static final int MAX_CONNECTIONS_PER_ROUTE = 50;
    private static final int CONNECTION_TIMEOUT_SECONDS = 10;
    private static final int READ_TIMEOUT_SECONDS = 25;
    private static final int CONNECTION_REQUEST_TIMEOUT_SECONDS = 10;

    /**
     * Service Account Token Provider Bean
     * <p>
     * Provides service account JWT tokens with automatic caching and refresh.
     * This bean is automatically configured if {@link RestTemplate} is available.
     *
     * @param builder       RestTemplateBuilder for token endpoint calls
     * @param tokenEndpoint Keycloak token endpoint URL
     * @param clientId      Service account client ID
     * @param clientSecret  Service account client secret
     * @return ServiceAccountTokenProvider instance
     */
    @Bean
    public ServiceAccountTokenProvider serviceAccountTokenProvider(RestTemplateBuilder builder,
                                                                   @Value("${keycloak.service-account.token-endpoint:http://localhost:7080/realms/wms-realm/protocol/openid"
                                                                           + "-connect/token}")
                                                                   String tokenEndpoint, @Value("${keycloak.service-account.client-id:wms-service-account}") String clientId,
                                                                   @Value("${keycloak.service-account.client-secret:}") String clientSecret) {
        // Create a dedicated RestTemplate for token endpoint calls
        // This RestTemplate does NOT have the authentication interceptor to avoid circular dependency
        RestTemplate tokenRestTemplate = createTokenEndpointRestTemplate(builder);

        log.info("Configuring service account token provider for inter-service authentication");
        return new ServiceAccountTokenProvider(tokenRestTemplate, tokenEndpoint, clientId, clientSecret);
    }

    /**
     * Creates a RestTemplate with connection pooling for the token endpoint.
     * This RestTemplate does NOT have authentication interceptor to avoid circular dependency.
     *
     * @param builder RestTemplateBuilder
     * @return RestTemplate for token endpoint calls
     */
    private RestTemplate createTokenEndpointRestTemplate(RestTemplateBuilder builder) {
        return createRestTemplateWithConnectionPool(builder);
    }

    /**
     * Creates a RestTemplate with connection pooling and timeouts.
     *
     * @param builder RestTemplateBuilder
     * @return RestTemplate with connection pool
     */
    private RestTemplate createRestTemplateWithConnectionPool(RestTemplateBuilder builder) {
        // Create connection pool manager
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);

        // Configure request timeouts
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
     * Service Account Authentication Interceptor Bean
     * <p>
     * Interceptor that automatically injects authentication headers for inter-service calls.
     *
     * @param tokenProvider Service account token provider
     * @return ServiceAccountAuthenticationInterceptor instance
     */
    @Bean
    public ServiceAccountAuthenticationInterceptor serviceAccountAuthenticationInterceptor(ServiceAccountTokenProvider tokenProvider) {
        log.info("Configuring service account authentication interceptor");
        return new ServiceAccountAuthenticationInterceptor(tokenProvider);
    }

    /**
     * Configuration Post-Processor to add authentication interceptor to RestTemplate beans
     * <p>
     * This bean post-processor automatically adds the service account authentication interceptor
     * to any RestTemplate bean created in the application context. This allows services to
     * define their own RestTemplate beans (e.g., with @LoadBalanced for Eureka) while
     * automatically getting authentication support.
     * <p>
     * Note: Services should create their own RestTemplate beans as needed. This configuration
     * will automatically add the authentication interceptor to them.
     *
     * @param interceptor Authentication interceptor
     * @return BeanPostProcessor that adds interceptor to RestTemplate beans
     */
    @Bean
    public org.springframework.beans.factory.config.BeanPostProcessor restTemplateAuthenticationPostProcessor(ServiceAccountAuthenticationInterceptor interceptor) {

        return new org.springframework.beans.factory.config.BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof RestTemplate) {
                    RestTemplate restTemplate = (RestTemplate) bean;

                    // Add authentication interceptor if not already present
                    if (restTemplate.getInterceptors().stream().noneMatch(i -> i instanceof ServiceAccountAuthenticationInterceptor)) {
                        restTemplate.getInterceptors().add(interceptor);
                        log.info("Added service account authentication interceptor to RestTemplate bean: {}", beanName);
                    }
                }
                return bean;
            }
        };
    }
}
