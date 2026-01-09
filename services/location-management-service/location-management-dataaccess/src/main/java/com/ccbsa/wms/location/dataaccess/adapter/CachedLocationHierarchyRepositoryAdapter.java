package com.ccbsa.wms.location.dataaccess.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.application.service.port.data.LocationViewRepository;
import com.ccbsa.wms.location.application.service.port.data.dto.LocationView;
import com.ccbsa.wms.location.application.service.port.service.LocationHierarchyCacheInvalidationPort;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cached Location Hierarchy Repository Adapter.
 * <p>
 * Decorates LocationViewRepositoryAdapter with Spring Cache (Caffeine) for hierarchy queries.
 * Implements cache-aside pattern for reads.
 * <p>
 * Cache Configuration:
 * - Cache names: "warehouses", "zones", "aisles", "racks", "bins"
 * - Cache keys: "{tenantId}", "{warehouseId}", "{zoneId}", "{aisleId}", "{rackId}"
 * - TTL: 5 minutes (configured in CacheConfig)
 * - Maximum size: 1000 entries (configured in CacheConfig)
 * <p>
 * Cache Eviction:
 * - Cache is evicted on location create/update/delete events via LocationCacheInvalidationListener
 * <p>
 * Spring Configuration:
 * - @Primary: Ensures this adapter is injected instead of base adapter
 * - @Repository: Marks as Spring Data repository component
 */
@Repository
@Primary
@Slf4j
@RequiredArgsConstructor
public class CachedLocationHierarchyRepositoryAdapter implements LocationViewRepository, LocationHierarchyCacheInvalidationPort {
    private final LocationViewRepositoryAdapter baseRepository;

    @Override
    public Optional<LocationView> findByTenantIdAndId(TenantId tenantId, LocationId locationId) {
        // Individual location lookups are fast, not cached to avoid cache bloat
        return baseRepository.findByTenantIdAndId(tenantId, locationId);
    }

    @Override
    public List<LocationView> findByTenantIdWithFilters(TenantId tenantId, String zone, String status, String search, int page, int size) {
        // Filtered queries with pagination are not cached to avoid cache bloat
        return baseRepository.findByTenantIdWithFilters(tenantId, zone, status, search, page, size);
    }

    @Override
    public long countByTenantIdWithFilters(TenantId tenantId, String zone, String status, String search) {
        // Count queries are fast, not cached
        return baseRepository.countByTenantIdWithFilters(tenantId, zone, status, search);
    }

    @Override
    public List<LocationView> findAvailableLocations(TenantId tenantId) {
        // Available locations query is not cached to avoid cache bloat
        return baseRepository.findAvailableLocations(tenantId);
    }

    @Override
    @Cacheable(value = "warehouses", key = "#tenantId.value", cacheManager = "hierarchyCacheManager")
    public List<LocationView> findWarehousesByTenantId(TenantId tenantId) {
        log.debug("Cache miss for warehouses: {}", tenantId);
        return baseRepository.findWarehousesByTenantId(tenantId);
    }

    @Override
    @Cacheable(value = "zones", key = "#warehouseId.value", cacheManager = "hierarchyCacheManager")
    public List<LocationView> findZonesByWarehouseId(TenantId tenantId, LocationId warehouseId) {
        log.debug("Cache miss for zones: {}", warehouseId);
        return baseRepository.findZonesByWarehouseId(tenantId, warehouseId);
    }

    @Override
    @Cacheable(value = "aisles", key = "#zoneId.value", cacheManager = "hierarchyCacheManager")
    public List<LocationView> findAislesByZoneId(TenantId tenantId, LocationId zoneId) {
        log.debug("Cache miss for aisles: {}", zoneId);
        return baseRepository.findAislesByZoneId(tenantId, zoneId);
    }

    @Override
    @Cacheable(value = "racks", key = "#aisleId.value", cacheManager = "hierarchyCacheManager")
    public List<LocationView> findRacksByAisleId(TenantId tenantId, LocationId aisleId) {
        log.debug("Cache miss for racks: {}", aisleId);
        return baseRepository.findRacksByAisleId(tenantId, aisleId);
    }

    @Override
    @Cacheable(value = "bins", key = "#rackId.value", cacheManager = "hierarchyCacheManager")
    public List<LocationView> findBinsByRackId(TenantId tenantId, LocationId rackId) {
        log.debug("Cache miss for bins: {}", rackId);
        return baseRepository.findBinsByRackId(tenantId, rackId);
    }

    @Override
    public List<LocationView> findByTenantIdAndType(TenantId tenantId, String type) {
        // Type-based queries are used internally for aggregation, not cached
        return baseRepository.findByTenantIdAndType(tenantId, type);
    }

    /**
     * Evicts warehouse cache for a tenant.
     * Called by LocationCacheInvalidationListener on location events.
     *
     * @param tenantId Tenant ID
     */
    @CacheEvict(value = "warehouses", key = "#tenantId.value", cacheManager = "hierarchyCacheManager")
    public void evictWarehouses(TenantId tenantId) {
        log.debug("Evicting warehouses cache for tenant: {}", tenantId);
    }

    /**
     * Evicts zone cache for a warehouse.
     * Called by LocationCacheInvalidationListener on location events.
     *
     * @param warehouseId Warehouse location ID
     */
    @CacheEvict(value = "zones", key = "#warehouseId.value", cacheManager = "hierarchyCacheManager")
    public void evictZones(LocationId warehouseId) {
        log.debug("Evicting zones cache for warehouse: {}", warehouseId);
    }

    /**
     * Evicts aisle cache for a zone.
     * Called by LocationCacheInvalidationListener on location events.
     *
     * @param zoneId Zone location ID
     */
    @CacheEvict(value = "aisles", key = "#zoneId.value", cacheManager = "hierarchyCacheManager")
    public void evictAisles(LocationId zoneId) {
        log.debug("Evicting aisles cache for zone: {}", zoneId);
    }

    /**
     * Evicts rack cache for an aisle.
     * Called by LocationCacheInvalidationListener on location events.
     *
     * @param aisleId Aisle location ID
     */
    @CacheEvict(value = "racks", key = "#aisleId.value", cacheManager = "hierarchyCacheManager")
    public void evictRacks(LocationId aisleId) {
        log.debug("Evicting racks cache for aisle: {}", aisleId);
    }

    /**
     * Evicts bin cache for a rack.
     * Called by LocationCacheInvalidationListener on location events.
     *
     * @param rackId Rack location ID
     */
    @CacheEvict(value = "bins", key = "#rackId.value", cacheManager = "hierarchyCacheManager")
    public void evictBins(LocationId rackId) {
        log.debug("Evicting bins cache for rack: {}", rackId);
    }

    /**
     * Evicts all hierarchy caches.
     * Called by LocationCacheInvalidationListener on major location events.
     */
    @CacheEvict(value = {"warehouses", "zones", "aisles", "racks", "bins"}, allEntries = true, cacheManager = "hierarchyCacheManager")
    public void evictAll() {
        log.debug("Evicting all hierarchy caches");
    }
}
