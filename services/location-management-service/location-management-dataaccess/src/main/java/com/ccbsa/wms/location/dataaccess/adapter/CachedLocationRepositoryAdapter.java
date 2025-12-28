package com.ccbsa.wms.location.dataaccess.adapter;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.cache.decorator.CachedRepositoryDecorator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.application.service.port.repository.LocationRepository;
import com.ccbsa.wms.location.domain.core.entity.Location;
import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Cached Location Repository Adapter.
 * <p>
 * MANDATORY: Decorates LocationRepositoryAdapter with Redis caching.
 * Implements cache-aside pattern for reads and write-through for writes.
 * <p>
 * Cache Configuration:
 * - Namespace: "locations"
 * - TTL: Configured via application.yml (default: 30 minutes)
 * - Eviction: LRU (configured in Redis)
 * <p>
 * Spring Configuration:
 * - @Primary: Ensures this adapter is injected instead of base adapter
 * - @Repository: Marks as Spring Data repository component
 */
@Repository
@Primary
@Slf4j
public class CachedLocationRepositoryAdapter extends CachedRepositoryDecorator<Location, LocationId> implements LocationRepository {
    private final LocationRepositoryAdapter baseRepository;

    public CachedLocationRepositoryAdapter(LocationRepositoryAdapter baseRepository, RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        super(baseRepository, redisTemplate, CacheNamespace.LOCATIONS.getValue(), Duration.ofMinutes(30), // TTL from config
                meterRegistry);
        this.baseRepository = baseRepository;
    }

    @Override
    public Location save(Location location) {
        // Write-through: Save to database + update cache
        Location saved = baseRepository.save(location);

        if (saved.getTenantId() != null && saved.getId() != null) {
            saveWithCache(saved.getTenantId(), saved.getId().getValue(), saved, obj -> saved // Already saved
            );
        }

        return saved;
    }

    @Override
    public Optional<Location> findByIdAndTenantId(LocationId id, TenantId tenantId) {
        return findWithCache(tenantId, id.getValue(), entityId -> baseRepository.findByIdAndTenantId(id, tenantId));
    }

    @Override
    public boolean existsByBarcodeAndTenantId(LocationBarcode barcode, TenantId tenantId) {
        // Existence checks are fast, not cached
        return baseRepository.existsByBarcodeAndTenantId(barcode, tenantId);
    }

    @Override
    public Optional<Location> findByBarcodeAndTenantId(LocationBarcode barcode, TenantId tenantId) {
        // Barcode lookups are less frequent, not cached
        return baseRepository.findByBarcodeAndTenantId(barcode, tenantId);
    }

    @Override
    public boolean existsByCodeAndTenantId(String code, TenantId tenantId) {
        // Existence checks are fast, not cached
        return baseRepository.existsByCodeAndTenantId(code, tenantId);
    }

    @Override
    public List<Location> findByTenantId(TenantId tenantId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByTenantId(tenantId);
    }

    @Override
    public List<Location> findAvailableLocations(TenantId tenantId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findAvailableLocations(tenantId);
    }
}

