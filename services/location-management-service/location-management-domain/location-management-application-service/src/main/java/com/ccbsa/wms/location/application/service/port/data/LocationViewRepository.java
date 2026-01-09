package com.ccbsa.wms.location.application.service.port.data;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.application.service.port.data.dto.LocationView;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

/**
 * Data Port: LocationViewRepository
 * <p>
 * Read model repository for location queries. Provides optimized read access to location data.
 * <p>
 * This is a data port (read model) used by query handlers, not a repository port (write model).
 * <p>
 * Responsibilities:
 * - Provide optimized read model queries
 * - Support eventual consistency (read model may lag behind write model)
 * - Enable query performance optimization through denormalization
 */
public interface LocationViewRepository {

    /**
     * Finds a location view by tenant ID and location ID.
     *
     * @param tenantId   Tenant ID
     * @param locationId Location ID
     * @return Optional LocationView
     */
    Optional<LocationView> findByTenantIdAndId(TenantId tenantId, LocationId locationId);

    /**
     * Finds all location views for a tenant with optional filters and pagination.
     *
     * @param tenantId Tenant ID
     * @param zone     Optional zone filter (case-insensitive, null to ignore)
     * @param status   Optional status filter (null to ignore)
     * @param search   Optional search term (searches in barcode, zone, aisle, description)
     * @param page     Page number (0-based)
     * @param size     Page size
     * @return List of LocationView
     */
    List<LocationView> findByTenantIdWithFilters(TenantId tenantId, String zone, String status, String search, int page, int size);

    /**
     * Counts location views matching the filter criteria.
     *
     * @param tenantId Tenant ID
     * @param zone     Optional zone filter (case-insensitive, null to ignore)
     * @param status   Optional status filter (null to ignore)
     * @param search   Optional search term (searches in barcode, zone, aisle, description)
     * @return Total count
     */
    long countByTenantIdWithFilters(TenantId tenantId, String zone, String status, String search);

    /**
     * Finds available location views for a tenant.
     * <p>
     * Available locations are those with status AVAILABLE or RESERVED.
     *
     * @param tenantId Tenant ID
     * @return List of LocationView
     */
    List<LocationView> findAvailableLocations(TenantId tenantId);

    /**
     * Finds all warehouses (locations with type WAREHOUSE) for a tenant.
     *
     * @param tenantId Tenant ID
     * @return List of LocationView
     */
    List<LocationView> findWarehousesByTenantId(TenantId tenantId);

    /**
     * Finds all zones (locations with type ZONE) under a warehouse.
     *
     * @param tenantId    Tenant ID
     * @param warehouseId Warehouse location ID
     * @return List of LocationView
     */
    List<LocationView> findZonesByWarehouseId(TenantId tenantId, LocationId warehouseId);

    /**
     * Finds all aisles (locations with type AISLE) under a zone.
     *
     * @param tenantId Tenant ID
     * @param zoneId   Zone location ID
     * @return List of LocationView
     */
    List<LocationView> findAislesByZoneId(TenantId tenantId, LocationId zoneId);

    /**
     * Finds all racks (locations with type RACK) under an aisle.
     *
     * @param tenantId Tenant ID
     * @param aisleId  Aisle location ID
     * @return List of LocationView
     */
    List<LocationView> findRacksByAisleId(TenantId tenantId, LocationId aisleId);

    /**
     * Finds all bins (locations with type BIN) under a rack.
     *
     * @param tenantId Tenant ID
     * @param rackId   Rack location ID
     * @return List of LocationView
     */
    List<LocationView> findBinsByRackId(TenantId tenantId, LocationId rackId);

    /**
     * Finds all locations of a specific type for a tenant.
     *
     * @param tenantId Tenant ID
     * @param type     Location type (WAREHOUSE, ZONE, AISLE, RACK, BIN)
     * @return List of LocationView
     */
    List<LocationView> findByTenantIdAndType(TenantId tenantId, String type);
}

