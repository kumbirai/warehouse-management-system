package com.ccbsa.wms.user.cache;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ccbsa.common.cache.warming.CacheWarmingService;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.user.application.service.port.repository.UserRepository;
import com.ccbsa.wms.user.domain.core.entity.User;

/**
 * User Service Cache Warming.
 * <p>
 * Warms user caches for tenants that have users in the database on startup.
 * <p>
 * Warming Strategy:
 * 1. Query database for distinct tenant IDs that have users
 * 2. For each tenant, load all users for that tenant
 * 3. Trigger cache population via findByTenantIdAndId calls
 * 4. Users are automatically cached via CachedUserRepositoryAdapter
 * <p>
 * This reduces initial response latency for first user requests after deployment.
 * <p>
 * Note: This implementation queries the database directly to find tenants with users,
 * avoiding dependency on tenant service during startup. Cache warming failures are
 * handled gracefully and do not prevent application startup.
 */
@Service
public class UserCacheWarmingService
        extends CacheWarmingService {

    private static final Logger log = LoggerFactory.getLogger(UserCacheWarmingService.class);

    private final UserRepository userRepository;

    public UserCacheWarmingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void performCacheWarming() {
        log.info("Warming user caches...");

        try {
            // Query all users across all tenants to discover tenant IDs
            // This uses findAllAcrossTenants which queries all tenant schemas
            List<User> allUsers = userRepository.findAllAcrossTenants(null);

            if (allUsers.isEmpty()) {
                log.info("No users found in database - skipping cache warming");
                return;
            }

            // Extract distinct tenant IDs
            Set<TenantId> tenantIds = allUsers.stream()
                    .map(User::getTenantId)
                    .collect(Collectors.toSet());

            log.info("Found {} tenants with users - warming caches", tenantIds.size());

            // Warm cache for each tenant
            int totalWarmed = 0;
            for (TenantId tenantId : tenantIds) {
                try {
                    int warmed = warmTenantUserCache(tenantId);
                    totalWarmed += warmed;
                } catch (Exception e) {
                    log.warn("Failed to warm cache for tenant: {} - continuing with next tenant",
                            tenantId.getValue(), e);
                    // Continue with next tenant - don't fail entire warming process
                }
            }

            log.info("User cache warming completed - warmed {} users across {} tenants",
                    totalWarmed, tenantIds.size());
        } catch (Exception e) {
            log.error("Cache warming failed - continuing startup", e);
            // Don't throw - warming failure shouldn't prevent startup
        }
    }

    /**
     * Warms user cache for a specific tenant.
     * <p>
     * Loads all users for the tenant and triggers cache population by calling
     * findByTenantIdAndId for each user. This ensures users are cached and
     * available for fast retrieval.
     *
     * @param tenantId Tenant ID to warm cache for
     * @return Number of users warmed for this tenant
     */
    private int warmTenantUserCache(TenantId tenantId) {
        try {
            // Load all users for this tenant
            List<User> users = userRepository.findByTenantId(tenantId);

            if (users.isEmpty()) {
                log.debug("No users found for tenant: {} - skipping cache warming", tenantId.getValue());
                return 0;
            }

            // Trigger cache population by calling findByTenantIdAndId for each user
            // This ensures users are cached via CachedUserRepositoryAdapter
            users.forEach(user -> {
                try {
                    userRepository.findByTenantIdAndId(tenantId, user.getId());
                } catch (Exception e) {
                    log.debug("Failed to cache user {} for tenant {}: {}",
                            user.getId().getValue(), tenantId.getValue(), e.getMessage());
                    // Continue with next user
                }
            });

            log.debug("Warmed cache for {} users in tenant: {}", users.size(), tenantId.getValue());
            return users.size();
        } catch (Exception e) {
            log.warn("Failed to warm cache for tenant: {}", tenantId.getValue(), e);
            throw e; // Re-throw to be caught by caller
        }
    }
}
