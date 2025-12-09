package com.ccbsa.wms.common.security;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;

/**
 * Thread-local tenant context holder.
 * Stores tenant ID and user ID for the current request thread.
 */
public final class TenantContext {
    private static final ThreadLocal<TenantId> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<UserId> USER_ID = new ThreadLocal<>();

    private TenantContext() {
        // Utility class
    }

    /**
     * Gets the tenant ID for the current thread.
     *
     * @return the tenant ID, or null if not set
     */
    public static TenantId getTenantId() {
        return TENANT_ID.get();
    }

    /**
     * Sets the tenant ID for the current thread.
     *
     * @param tenantId the tenant ID
     */
    public static void setTenantId(TenantId tenantId) {
        TENANT_ID.set(tenantId);
    }

    /**
     * Gets the user ID for the current thread.
     *
     * @return the user ID, or null if not set
     */
    public static UserId getUserId() {
        return USER_ID.get();
    }

    /**
     * Sets the user ID for the current thread.
     *
     * @param userId the user ID
     */
    public static void setUserId(UserId userId) {
        USER_ID.set(userId);
    }

    /**
     * Clears the tenant context for the current thread.
     * Should be called after request processing to prevent memory leaks.
     */
    public static void clear() {
        TENANT_ID.remove();
        USER_ID.remove();
    }
}

