package com.ccbsa.wms.user.application.service.port.service;

import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Port interface for tenant service operations.
 * <p>
 * Defines the contract for tenant validation and status checks. Implemented by the messaging layer (user-messaging).
 */
public interface TenantServicePort {
    /**
     * Checks if a tenant is active.
     *
     * @param tenantId Tenant identifier
     * @return true if tenant exists and is ACTIVE, false otherwise
     * @throws com.ccbsa.wms.user.application.service.exception.TenantNotFoundException if tenant does not exist
     * @throws com.ccbsa.wms.user.application.service.exception.TenantServiceException  if service call fails
     */
    boolean isTenantActive(TenantId tenantId);

    /**
     * Gets the status of a tenant.
     *
     * @param tenantId Tenant identifier
     * @return Tenant status (ACTIVE, INACTIVE, SUSPENDED)
     * @throws com.ccbsa.wms.user.application.service.exception.TenantNotFoundException if tenant does not exist
     * @throws com.ccbsa.wms.user.application.service.exception.TenantServiceException  if service call fails
     */
    String getTenantStatus(TenantId tenantId);

    /**
     * Gets tenant information including name.
     *
     * @param tenantId Tenant identifier
     * @return Tenant information or empty if tenant does not exist
     * @throws com.ccbsa.wms.user.application.service.exception.TenantServiceException if service call fails
     */
    Optional<TenantInfo> getTenantInfo(TenantId tenantId);

    /**
     * Tenant information DTO.
     */
    record TenantInfo(TenantId tenantId, String name, TenantStatus status) {
        public enum TenantStatus {
            PENDING, ACTIVE, INACTIVE, SUSPENDED
        }
    }
}

