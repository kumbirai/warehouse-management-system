package com.ccbsa.wms.stock.dataaccess.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.service.port.data.StockLevelThresholdViewRepository;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockLevelThresholdView;
import com.ccbsa.wms.stock.application.service.port.repository.StockLevelThresholdRepository;
import com.ccbsa.wms.stock.domain.core.entity.StockLevelThreshold;
import com.ccbsa.wms.stock.domain.core.valueobject.StockLevelThresholdId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: StockLevelThresholdViewRepositoryAdapter
 * <p>
 * Implements StockLevelThresholdViewRepository data port interface.
 * <p>
 * This is a transitional adapter that wraps the write model repository for read-only operations.
 * In future iterations, this should be replaced with a proper read model implementation.
 * <p>
 * CQRS Compliance: While this adapter uses the write model repository internally,
 * it exposes only read-only operations and returns view DTOs, maintaining the CQRS interface contract.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class StockLevelThresholdViewRepositoryAdapter implements StockLevelThresholdViewRepository {
    private final StockLevelThresholdRepository thresholdRepository;

    @Override
    public Optional<StockLevelThresholdView> findByTenantIdAndId(TenantId tenantId, StockLevelThresholdId thresholdId) {
        return thresholdRepository.findByIdAndTenantId(thresholdId, tenantId).map(this::toView);
    }

    /**
     * Maps StockLevelThreshold domain entity to StockLevelThresholdView DTO.
     *
     * @param threshold Domain entity
     * @return View DTO
     */
    private StockLevelThresholdView toView(StockLevelThreshold threshold) {
        return StockLevelThresholdView.builder().thresholdId(threshold.getId()).tenantId(threshold.getTenantId().getValue()).productId(threshold.getProductId())
                .locationId(threshold.getLocationId()).minimumQuantity(threshold.getMinimumQuantity() != null ? threshold.getMinimumQuantity().getValue() : null)
                .maximumQuantity(threshold.getMaximumQuantity() != null ? threshold.getMaximumQuantity().getValue() : null).enableAutoRestock(threshold.isEnableAutoRestock())
                .build();
    }

    @Override
    public Optional<StockLevelThresholdView> findByTenantIdAndProductIdAndLocationId(TenantId tenantId, ProductId productId, LocationId locationId) {
        return thresholdRepository.findByTenantIdAndProductIdAndLocationId(tenantId, productId, locationId).map(this::toView);
    }

    @Override
    public List<StockLevelThresholdView> findByTenantId(TenantId tenantId) {
        return thresholdRepository.findByTenantId(tenantId).stream().map(this::toView).collect(Collectors.toList());
    }

    @Override
    public List<StockLevelThresholdView> findByTenantIdAndProductId(TenantId tenantId, ProductId productId) {
        return thresholdRepository.findByTenantIdAndProductId(tenantId, productId).stream().map(this::toView).collect(Collectors.toList());
    }
}
