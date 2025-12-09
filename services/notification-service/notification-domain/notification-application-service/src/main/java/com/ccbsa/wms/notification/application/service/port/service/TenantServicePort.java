package com.ccbsa.wms.notification.application.service.port.service;

import java.util.Optional;

import com.ccbsa.common.domain.valueobject.EmailAddress;
import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Port: TenantServicePort
 * <p>
 * Defines the contract for retrieving tenant information from tenant-service.
 * Implemented by service adapters in infrastructure layers.
 */
public interface TenantServicePort {

    /**
     * Gets the email address for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return EmailAddress address of the tenant
     * @throws RuntimeException if tenant not found or email retrieval fails
     */
    EmailAddress getTenantEmail(TenantId tenantId);

    /**
     * Gets full tenant details for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return Optional containing tenant details, or empty if tenant not found
     * @throws RuntimeException if tenant service is unavailable
     */
    Optional<TenantDetails> getTenantDetails(TenantId tenantId);

    /**
     * Tenant details DTO.
     */
    record TenantDetails(
            String tenantId,
            String name,
            String status,
            String emailAddress,
            String phone,
            String address) {
    }
}

