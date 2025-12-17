package com.ccbsa.wms.location.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ccbsa.wms.location.dataaccess.entity.LocationEntity;

/**
 * JPA Repository: LocationJpaRepository
 * <p>
 * Spring Data JPA repository for LocationEntity. Provides database access methods with multi-tenant support.
 */
public interface LocationJpaRepository
        extends JpaRepository<LocationEntity, UUID> {
    /**
     * Finds a location by tenant ID and location ID.
     *
     * @param tenantId Tenant identifier
     * @param id       Location identifier
     * @return Optional LocationEntity if found
     */
    Optional<LocationEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Checks if a location exists with the given barcode for the tenant.
     *
     * @param tenantId Tenant identifier
     * @param barcode  Location barcode
     * @return true if location exists
     */
    boolean existsByTenantIdAndBarcode(String tenantId, String barcode);

    /**
     * Finds all locations for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return List of LocationEntity for the tenant
     */
    List<LocationEntity> findByTenantId(String tenantId);

    /**
     * Finds locations by tenant ID and status.
     *
     * @param tenantId Tenant identifier
     * @param status   Location status
     * @return List of LocationEntity matching the criteria
     */
    List<LocationEntity> findByTenantIdAndStatus(String tenantId, com.ccbsa.wms.location.domain.core.valueobject.LocationStatus status);

    /**
     * Finds locations by tenant ID and zone.
     *
     * @param tenantId Tenant identifier
     * @param zone     Zone identifier
     * @return List of LocationEntity matching the criteria
     */
    List<LocationEntity> findByTenantIdAndZone(String tenantId, String zone);
}

