package com.ccbsa.wms.location.application.service.port.repository;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.entity.Location;
import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

/**
 * Repository Port: LocationRepository
 * <p>
 * Defines the contract for Location aggregate persistence. Implemented by data access adapters.
 * <p>
 * This port is defined in the application service layer (not domain core) to maintain proper dependency direction in hexagonal architecture.
 */
public interface LocationRepository {
    /**
     * Saves a Location aggregate.
     * <p>
     * Creates a new location if it doesn't exist, or updates an existing one.
     *
     * @param location Location aggregate to save
     * @return Saved Location aggregate (may be a new instance from mapper)
     */
    Location save(Location location);

    /**
     * Finds a Location by ID and tenant ID.
     *
     * @param id       Location identifier
     * @param tenantId Tenant identifier
     * @return Optional Location if found
     */
    Optional<Location> findByIdAndTenantId(LocationId id, TenantId tenantId);

    /**
     * Checks if a location with the given barcode exists for the tenant.
     *
     * @param barcode  Location barcode
     * @param tenantId Tenant identifier
     * @return true if location exists with the barcode
     */
    boolean existsByBarcodeAndTenantId(LocationBarcode barcode, TenantId tenantId);

    /**
     * Finds a Location by barcode and tenant ID.
     *
     * @param barcode  Location barcode
     * @param tenantId Tenant identifier
     * @return Optional Location if found
     */
    Optional<Location> findByBarcodeAndTenantId(LocationBarcode barcode, TenantId tenantId);

    /**
     * Checks if a location with the given code exists for the tenant.
     *
     * @param code     Location code
     * @param tenantId Tenant identifier
     * @return true if location exists with the code
     */
    boolean existsByCodeAndTenantId(String code, TenantId tenantId);

    /**
     * Finds all locations for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return List of locations for the tenant
     */
    List<Location> findByTenantId(TenantId tenantId);

    /**
     * Finds available locations for a tenant (status = AVAILABLE or RESERVED).
     *
     * @param tenantId Tenant identifier
     * @return List of available locations
     */
    List<Location> findAvailableLocations(TenantId tenantId);
}

