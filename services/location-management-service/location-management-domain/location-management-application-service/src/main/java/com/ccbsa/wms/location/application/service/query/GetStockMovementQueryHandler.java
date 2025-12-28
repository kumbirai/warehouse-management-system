package com.ccbsa.wms.location.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.location.application.service.port.data.StockMovementViewRepository;
import com.ccbsa.wms.location.application.service.query.dto.GetStockMovementQuery;
import com.ccbsa.wms.location.application.service.query.dto.StockMovementQueryResult;
import com.ccbsa.wms.location.domain.core.exception.StockMovementNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: GetStockMovementQueryHandler
 * <p>
 * Handles queries for retrieving a single stock movement read model.
 * <p>
 * Responsibilities:
 * - Loads StockMovement view from data port (read model)
 * - Maps view to query result DTO
 * <p>
 * Uses data port (StockMovementViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GetStockMovementQueryHandler {
    private final StockMovementViewRepository viewRepository;

    @Transactional(readOnly = true)
    public StockMovementQueryResult handle(GetStockMovementQuery query) {
        log.debug("Handling GetStockMovementQuery for movement: {}", query.getStockMovementId());

        // 1. Validate query
        validateQuery(query);

        // 2. Load read model (view) from data port
        var movementView = viewRepository.findByTenantIdAndId(query.getTenantId(), query.getStockMovementId())
                .orElseThrow(() -> new StockMovementNotFoundException("Stock movement not found: " + query.getStockMovementId().getValueAsString()));

        // 3. Map view to query result
        return mapToQueryResult(movementView);
    }

    /**
     * Validates query before execution.
     *
     * @param query Query to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateQuery(GetStockMovementQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (query.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (query.getStockMovementId() == null) {
            throw new IllegalArgumentException("StockMovementId is required");
        }
    }

    /**
     * Maps StockMovementView to StockMovementQueryResult DTO.
     *
     * @param view StockMovementView read model
     * @return StockMovementQueryResult DTO
     */
    private StockMovementQueryResult mapToQueryResult(com.ccbsa.wms.location.application.service.port.data.dto.StockMovementView view) {
        return StockMovementQueryResult.builder().stockMovementId(view.getStockMovementId()).stockItemId(view.getStockItemId()).productId(view.getProductId())
                .sourceLocationId(view.getSourceLocationId()).destinationLocationId(view.getDestinationLocationId()).quantity(view.getQuantity())
                .movementType(view.getMovementType()).reason(view.getReason()).status(view.getStatus()).initiatedBy(view.getInitiatedBy()).initiatedAt(view.getInitiatedAt())
                .completedBy(view.getCompletedBy()).completedAt(view.getCompletedAt()).cancelledBy(view.getCancelledBy()).cancelledAt(view.getCancelledAt())
                .cancellationReason(view.getCancellationReason()).build();
    }
}

