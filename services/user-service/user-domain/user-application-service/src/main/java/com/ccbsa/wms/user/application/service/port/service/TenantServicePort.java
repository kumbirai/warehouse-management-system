package com.ccbsa.wms.user.application.service.port.service;

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
}

