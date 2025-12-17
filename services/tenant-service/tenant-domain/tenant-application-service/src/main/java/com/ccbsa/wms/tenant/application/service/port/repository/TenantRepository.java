package com.ccbsa.wms.tenant.application.service.port.repository;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.domain.core.entity.Tenant;

/**
 * Repository port for Tenant aggregate persistence.
 * <p>
 * This port is defined in the application service layer and implemented by the data access layer (tenant-dataaccess).
 */
public interface TenantRepository {
    /**
     * Saves a tenant aggregate.
     *
     * @param tenant Tenant aggregate to save
     */
    void save(Tenant tenant);

    /**
     * Finds a tenant by ID.
     *
     * @param tenantId Tenant identifier
     * @return Tenant if found, empty otherwise
     */
    Optional<Tenant> findById(TenantId tenantId);

    /**
     * Finds all tenants.
     *
     * @return List of all tenants
     */
    List<Tenant> findAll();

    /**
     * Checks if a tenant exists by ID.
     *
     * @param tenantId Tenant identifier
     * @return true if tenant exists
     */
    boolean existsById(TenantId tenantId);

    /**
     * Deletes a tenant by ID.
     *
     * @param tenantId Tenant identifier
     */
    void deleteById(TenantId tenantId);
}

