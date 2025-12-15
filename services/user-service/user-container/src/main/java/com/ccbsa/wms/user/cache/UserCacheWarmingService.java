package com.ccbsa.wms.user.cache;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ccbsa.common.cache.warming.CacheWarmingService;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.user.application.service.port.repository.UserRepository;
import com.ccbsa.wms.user.domain.core.entity.User;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * User Service Cache Warming.
 * <p>
 * Warms user caches for active tenants on startup.
 * <p>
 * Warming Strategy:
 * 1. Load all active tenants
 * 2. For each tenant, load frequently accessed users (e.g., admins)
 * 3. Users are automatically cached via CachedUserRepositoryAdapter
 * <p>
 * This reduces initial response latency for first user requests after deployment.
 */
@Service
public class UserCacheWarmingService extends CacheWarmingService {

    private static final Logger log = LoggerFactory.getLogger(UserCacheWarmingService.class);

    private final UserRepository userRepository;
    // Note: TenantRepository would be injected here if available
    // For now, we'll warm caches for known tenants or skip if not available

    public UserCacheWarmingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void performCacheWarming() {
        log.info("Warming user caches...");

        // Example: Warm caches for top 10 active tenants
        // In a real implementation, you would query tenant service for active tenants
        // For now, we'll skip warming if no tenant context is available
        // This is a placeholder - actual implementation would:
        // 1. Query tenant service for active tenants
        // 2. For each tenant, load frequently accessed users
        // 3. Trigger cache population via findByTenantIdAndId calls

        log.info("User cache warming completed (placeholder - implement tenant query logic)");
    }

    /**
     * Warms user cache for a specific tenant.
     * <p>
     * This method is a placeholder for future implementation.
     * It will be called from performCacheWarming() once tenant query logic is implemented.
     *
     * @param tenantId Tenant ID to warm cache for
     */
    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "This is a placeholder method for future implementation. It will be called from performCacheWarming() once tenant query logic is implemented to "
                    + "retrieve active tenants.")
    private void warmTenantUserCache(TenantId tenantId) {
        try {
            // Load frequently accessed users (e.g., active users)
            List<User> users = userRepository.findByTenantId(tenantId);

            // Users are automatically cached via findById in CachedUserRepositoryAdapter
            users.forEach(user -> {
                userRepository.findByTenantIdAndId(tenantId, user.getId());
            });

            log.debug("Warmed cache for {} users in tenant: {}",
                    users.size(), tenantId.getValue());
        } catch (Exception e) {
            log.warn("Failed to warm cache for tenant: {}", tenantId.getValue(), e);
            // Continue with next tenant
        }
    }
}
