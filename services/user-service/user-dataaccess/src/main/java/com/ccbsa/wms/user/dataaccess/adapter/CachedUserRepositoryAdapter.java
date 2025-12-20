package com.ccbsa.wms.user.dataaccess.adapter;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.cache.decorator.CachedRepositoryDecorator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.user.application.service.port.repository.UserRepository;
import com.ccbsa.wms.user.domain.core.entity.User;
import com.ccbsa.wms.user.domain.core.valueobject.UserStatus;
import com.ccbsa.wms.user.domain.core.valueobject.Username;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Cached User Repository Adapter.
 * <p>
 * Decorates UserRepositoryAdapter with Redis caching. Implements cache-aside pattern for reads and write-through for writes.
 * <p>
 * Cache Configuration: - Namespace: "users" - TTL: 15 minutes (configurable via application.yml) - Eviction: LRU (configured in Redis)
 * <p>
 * Spring Configuration: - @Primary: Ensures this adapter is injected instead of base adapter - @Repository: Marks as Spring Data repository component
 */
@Repository
@Primary
public class CachedUserRepositoryAdapter extends CachedRepositoryDecorator<User, UserId> implements UserRepository {

    private static final Logger log = LoggerFactory.getLogger(CachedUserRepositoryAdapter.class);

    private final UserRepositoryAdapter baseRepository;

    public CachedUserRepositoryAdapter(UserRepositoryAdapter baseRepository, RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        super(baseRepository, redisTemplate, CacheNamespace.USERS.getValue(), Duration.ofMinutes(15), // TTL from config
                meterRegistry);
        this.baseRepository = baseRepository;
    }

    /**
     * Finds user by ID with caching.
     * <p>
     * Cache key: tenant:{tenantId}:users:{userId}
     * <p>
     * Note: This method requires tenant context. Use findByTenantIdAndId for multi-tenant scenarios.
     */
    @Override
    public Optional<User> findById(UserId id) {
        // For findById without tenant, we can't cache properly
        // Delegate to base repository
        return baseRepository.findById(id);
    }

    /**
     * Finds user by tenant ID and user ID with caching.
     * <p>
     * This is the primary lookup method in multi-tenant architecture. Cache-aside pattern: 1. Check cache 2. If miss, load from database 3. Populate cache with TTL
     */
    @Override
    public Optional<User> findByTenantIdAndId(TenantId tenantId, UserId id) {
        return findWithCache(tenantId, id.getUuid(), entityId -> baseRepository.findByTenantIdAndId(tenantId, id));
    }

    /**
     * Finds all users for a tenant.
     * <p>
     * Note: Collection queries are NOT cached to avoid cache bloat. Use view repositories with pagination for large datasets.
     */
    @Override
    public List<User> findByTenantId(TenantId tenantId) {
        // Delegate to base repository - no caching for collections
        return baseRepository.findByTenantId(tenantId);
    }

    /**
     * Finds all users (for SYSTEM_ADMIN only).
     * <p>
     * Not cached - cross-tenant operation.
     */
    @Override
    public List<User> findAll() {
        return baseRepository.findAll();
    }

    /**
     * Finds all users across all tenant schemas (for SYSTEM_ADMIN only).
     * <p>
     * Not cached - cross-tenant operation.
     */
    @Override
    public List<User> findAllAcrossTenants(UserStatus status) {
        return baseRepository.findAllAcrossTenants(status);
    }

    /**
     * Finds all users across all tenant schemas with search filter (for SYSTEM_ADMIN only).
     * <p>
     * Not cached - cross-tenant operation with dynamic search.
     */
    @Override
    public List<User> findAllAcrossTenantsWithSearch(UserStatus status, String searchTerm) {
        return baseRepository.findAllAcrossTenantsWithSearch(status, searchTerm);
    }

    /**
     * Finds a user by ID across all tenant schemas (for SYSTEM_ADMIN only).
     * <p>
     * Not cached - cross-tenant operation.
     */
    @Override
    public Optional<User> findByIdAcrossTenants(UserId userId) {
        return baseRepository.findByIdAcrossTenants(userId);
    }

    /**
     * Finds a user by username.
     * <p>
     * Not cached - requires tenant context for proper caching. Use findByTenantIdAndUsername instead.
     */
    @Override
    public Optional<User> findByUsername(Username username) {
        return baseRepository.findByUsername(username);
    }

    /**
     * Finds a user by tenant ID and username.
     * <p>
     * Not cached - username-based lookups are less frequent and harder to cache.
     */
    @Override
    public Optional<User> findByTenantIdAndUsername(TenantId tenantId, Username username) {
        return baseRepository.findByTenantIdAndUsername(tenantId, username);
    }

    /**
     * Finds users by tenant ID and status.
     * <p>
     * Not cached - collection query.
     */
    @Override
    public List<User> findByTenantIdAndStatus(TenantId tenantId, UserStatus status) {
        return baseRepository.findByTenantIdAndStatus(tenantId, status);
    }

    /**
     * Finds users by tenant ID where username or email contains the search term.
     * <p>
     * Not cached - search queries are dynamic and less cacheable.
     */
    @Override
    public List<User> findByTenantIdAndSearchTerm(TenantId tenantId, String searchTerm) {
        return baseRepository.findByTenantIdAndSearchTerm(tenantId, searchTerm);
    }

    /**
     * Finds users by tenant ID and status where username or email contains the search term.
     * <p>
     * Not cached - search queries are dynamic and less cacheable.
     */
    @Override
    public List<User> findByTenantIdAndStatusAndSearchTerm(TenantId tenantId, UserStatus status, String searchTerm) {
        return baseRepository.findByTenantIdAndStatusAndSearchTerm(tenantId, status, searchTerm);
    }

    /**
     * Checks if user exists by ID.
     * <p>
     * Not cached - existence checks are fast.
     */
    @Override
    public boolean existsById(UserId id) {
        return baseRepository.existsById(id);
    }

    /**
     * Checks if user exists by tenant ID and user ID.
     * <p>
     * Not cached - existence checks are fast.
     */
    @Override
    public boolean existsByTenantIdAndUserId(TenantId tenantId, UserId userId) {
        return baseRepository.existsByTenantIdAndUserId(tenantId, userId);
    }

    /**
     * Saves user with write-through caching.
     * <p>
     * Write-through pattern: 1. Save to database (source of truth) 2. Update cache immediately 3. Domain event published by command handler triggers collection cache invalidation
     */
    @Override
    public void save(User user) {
        // Save to database first
        baseRepository.save(user);

        // Write-through cache update
        if (user.getTenantId() != null && user.getId() != null) {
            String cacheKey = com.ccbsa.common.cache.key.CacheKeyGenerator.forEntity(user.getTenantId(), CacheNamespace.USERS.getValue(), user.getId().getUuid());

            try {
                // Clear domain events before caching (domain events are transient and should not be cached)
                user.clearDomainEvents();

                redisTemplate.opsForValue().set(cacheKey, user, Duration.ofMinutes(15));
                log.trace("Cache WRITE (write-through) for key: {}", cacheKey);
            } catch (Exception e) {
                log.error("Cache write-through failed for key: {}", cacheKey, e);
                // Don't throw - cache failure shouldn't break application
            }
        }
    }

    /**
     * Deletes user with cache eviction.
     * <p>
     * Write-through delete: 1. Delete from database 2. Evict from cache
     */
    @Override
    public void deleteById(UserId id) {
        // Delete requires tenant context for cache eviction
        // For now, delegate to base repository
        // Cache will be invalidated via event listener
        baseRepository.deleteById(id);
    }

    /**
     * Finds a user by Keycloak user ID.
     * <p>
     * Not cached - Keycloak ID lookups are less frequent.
     */
    @Override
    public Optional<User> findByKeycloakUserId(String keycloakUserId) {
        return baseRepository.findByKeycloakUserId(keycloakUserId);
    }
}
