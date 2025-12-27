package com.ccbsa.wms.stock.dataaccess.adapter;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.cache.decorator.CachedRepositoryDecorator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stock.application.service.port.repository.StockConsignmentRepository;
import com.ccbsa.wms.stock.domain.core.entity.StockConsignment;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentReference;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Cached Stock Consignment Repository Adapter.
 * <p>
 * MANDATORY: Decorates StockConsignmentRepositoryAdapter with Redis caching.
 * Implements cache-aside pattern for reads and write-through for writes.
 * <p>
 * Cache Configuration:
 * - Namespace: "stock-consignments"
 * - TTL: Configured via application.yml (default: 30 minutes)
 * - Eviction: LRU (configured in Redis)
 * <p>
 * Spring Configuration:
 * - @Primary: Ensures this adapter is injected instead of base adapter
 * - @Repository: Marks as Spring Data repository component
 */
@Repository
@Primary
public class CachedStockConsignmentRepositoryAdapter extends CachedRepositoryDecorator<StockConsignment, ConsignmentId> implements StockConsignmentRepository {

    private static final Logger log = LoggerFactory.getLogger(CachedStockConsignmentRepositoryAdapter.class);

    private final StockConsignmentRepositoryAdapter baseRepository;

    public CachedStockConsignmentRepositoryAdapter(StockConsignmentRepositoryAdapter baseRepository, RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        super(baseRepository, redisTemplate, CacheNamespace.STOCK_CONSIGNMENTS.getValue(), Duration.ofMinutes(30), // TTL from config
                meterRegistry);
        this.baseRepository = baseRepository;
    }

    @Override
    public void save(StockConsignment consignment) {
        // Write-through: Save to database + update cache
        baseRepository.save(consignment);

        if (consignment.getTenantId() != null && consignment.getId() != null) {
            saveWithCache(consignment.getTenantId(), consignment.getId().getValue(), consignment, obj -> consignment // Already saved
            );
        }
    }

    @Override
    public Optional<StockConsignment> findByIdAndTenantId(ConsignmentId id, TenantId tenantId) {
        return findWithCache(tenantId, id.getValue(), entityId -> baseRepository.findByIdAndTenantId(id, tenantId));
    }

    @Override
    public Optional<StockConsignment> findByConsignmentReferenceAndTenantId(ConsignmentReference reference, TenantId tenantId) {
        // Reference lookups are less frequent, not cached
        return baseRepository.findByConsignmentReferenceAndTenantId(reference, tenantId);
    }

    @Override
    public boolean existsByConsignmentReferenceAndTenantId(ConsignmentReference reference, TenantId tenantId) {
        // Existence checks are fast, not cached
        return baseRepository.existsByConsignmentReferenceAndTenantId(reference, tenantId);
    }

    @Override
    public List<StockConsignment> findByTenantId(TenantId tenantId, int page, int size) {
        // List queries are not cached (too many combinations)
        return baseRepository.findByTenantId(tenantId, page, size);
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        // Count queries are not cached
        return baseRepository.countByTenantId(tenantId);
    }
}

