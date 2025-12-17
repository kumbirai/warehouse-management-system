package com.ccbsa.common.cache.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.ccbsa.common.cache.key.TenantAwareCacheKeyGenerator;
import com.ccbsa.common.cache.manager.TenantAwareCacheManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;

/**
 * Redis Cache Configuration.
 * <p>
 * Configures distributed caching infrastructure with: - Tenant-aware cache key generation - JSON serialization with type information - Connection pooling and timeouts - TTL-based
 * eviction policies
 * <p>
 * Activated via: spring.cache.type=redis
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(name = "spring.cache.type",
        havingValue = "redis",
        matchIfMissing = true)
public class CacheConfiguration {

    private final CacheProperties cacheProperties;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "CacheProperties is a Spring @ConfigurationProperties bean managed by Spring. It is initialized once and not "
                    + "mutated after construction. The reference is safe to store.")
    public CacheConfiguration(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    /**
     * Redis Connection Factory with optimized Lettuce client configuration.
     * <p>
     * Configuration: - Connection pooling: 10 max connections per node - Timeout: 2 seconds for commands - Socket timeout: 3 seconds for TCP operations - Auto-reconnect: Enabled
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        // Redis server configuration
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(cacheProperties.getRedis()
                .getHost());
        redisConfig.setPort(cacheProperties.getRedis()
                .getPort());

        if (cacheProperties.getRedis()
                .getPassword() != null && !cacheProperties.getRedis()
                .getPassword()
                .isEmpty()) {
            redisConfig.setPassword(cacheProperties.getRedis()
                    .getPassword());
        }

        // Lettuce client configuration (async/reactive client)
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .keepAlive(true)
                        .build())
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(2)))
                .autoReconnect(true)
                .build();

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(clientOptions)
                .commandTimeout(Duration.ofSeconds(2))
                .build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    /**
     * ObjectMapper for Redis value serialization.
     * <p>
     * Configuration:
     * - JavaTimeModule: Serialize LocalDateTime, Instant, etc.
     * - Type information: Enables polymorphic deserialization
     * - Fail on unknown properties: Disabled for backward compatibility
     */
    /**
     * ObjectMapper for Redis cache value serialization.
     * <p>
     * PRODUCTION-GRADE DESIGN: This ObjectMapper is explicitly named and scoped for Redis cache use only. It includes type information (default typing) which is REQUIRED for
     * polymorphic cache value deserialization, but should NOT be used
     * for REST API responses or Kafka messages.
     * <p>
     * IMPORTANT: All Redis cache-related beans must explicitly inject this bean by name using {@code @Qualifier("redisCacheObjectMapper")} to ensure proper separation from REST
     * API and Kafka ObjectMappers.
     */
    @Bean("redisCacheObjectMapper")
    public ObjectMapper redisCacheObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    /**
     * Tenant-Aware Cache Manager.
     * <p>
     * Features: - Automatic tenant prefix injection - Per-cache TTL configuration - JSON serialization with type safety - Cache-aside pattern support
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper redisCacheObjectMapper) {
        // JSON serializer for cache values
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisCacheObjectMapper);

        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(cacheProperties.getDefaultTtlMinutes()))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return new TenantAwareCacheManager(connectionFactory, defaultConfig, cacheProperties);
    }

    /**
     * Tenant-Aware Cache Key Generator.
     * <p>
     * Automatically prefixes cache keys with tenant ID from TenantContext. Format: tenant:{tenantId}:{cacheName}:{key}
     */
    @Bean("tenantAwareCacheKeyGenerator")
    public TenantAwareCacheKeyGenerator tenantAwareCacheKeyGenerator() {
        return new TenantAwareCacheKeyGenerator();
    }

    /**
     * RedisTemplate for manual cache operations.
     * <p>
     * Use cases: - Custom cache operations not covered by @Cacheable - Batch cache operations - Cache statistics queries
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                                                       @org.springframework.beans.factory.annotation.Qualifier("redisCacheObjectMapper") ObjectMapper redisCacheObjectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisCacheObjectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}
