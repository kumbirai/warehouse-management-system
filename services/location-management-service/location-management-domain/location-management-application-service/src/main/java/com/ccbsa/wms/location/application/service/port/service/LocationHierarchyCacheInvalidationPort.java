package com.ccbsa.wms.location.application.service.port.service;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

/**
 * Service Port: LocationHierarchyCacheInvalidationPort
 * <p>
 * Port for invalidating location hierarchy caches.
 * Used by event listeners to evict caches when locations are created/updated/deleted.
 */
public interface LocationHierarchyCacheInvalidationPort {

    /**
     * Evicts warehouse cache for a tenant.
     *
     * @param tenantId Tenant ID
     */
    void evictWarehouses(TenantId tenantId);

    /**
     * Evicts zone cache for a warehouse.
     *
     * @param warehouseId Warehouse location ID
     */
    void evictZones(LocationId warehouseId);

    /**
     * Evicts aisle cache for a zone.
     *
     * @param zoneId Zone location ID
     */
    void evictAisles(LocationId zoneId);

    /**
     * Evicts rack cache for an aisle.
     *
     * @param aisleId Aisle location ID
     */
    void evictRacks(LocationId aisleId);

    /**
     * Evicts bin cache for a rack.
     *
     * @param rackId Rack location ID
     */
    void evictBins(LocationId rackId);

    /**
     * Evicts all hierarchy caches.
     */
    void evictAll();
}
