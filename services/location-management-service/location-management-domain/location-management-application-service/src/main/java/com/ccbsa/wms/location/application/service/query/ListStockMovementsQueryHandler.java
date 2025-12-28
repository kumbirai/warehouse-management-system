package com.ccbsa.wms.location.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.location.application.service.port.data.StockMovementViewRepository;
import com.ccbsa.wms.location.application.service.query.dto.ListStockMovementsQuery;
import com.ccbsa.wms.location.application.service.query.dto.ListStockMovementsQueryResult;
import com.ccbsa.wms.location.application.service.query.dto.StockMovementQueryResult;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: ListStockMovementsQueryHandler
 * <p>
 * Handles queries for listing stock movement read models with optional filters.
 * <p>
 * Responsibilities:
 * - Loads StockMovement views from data port (read model) based on filters
 * - Maps views to query result DTOs
 * <p>
 * Uses data port (StockMovementViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ListStockMovementsQueryHandler {
    private final StockMovementViewRepository viewRepository;

    @Transactional(readOnly = true)
    public ListStockMovementsQueryResult handle(ListStockMovementsQuery query) {
        log.debug("Handling ListStockMovementsQuery for tenant: {}", query.getTenantId());

        // 1. Validate query
        validateQuery(query);

        // 2. Load read models (views) based on filters
        List<com.ccbsa.wms.location.application.service.port.data.dto.StockMovementView> movementViews = findMovements(query);

        // 3. Map views to query results
        @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE", justification = "results is used in builder - SpotBugs false positive") List<StockMovementQueryResult> results =
                movementViews.stream().map(this::mapToQueryResult).collect(Collectors.toList());

        // 4. Return result
        return ListStockMovementsQueryResult.builder().movements(results).totalCount(results.size()).build();
    }

    /**
     * Validates query before execution.
     *
     * @param query Query to validate
     * @throws IllegalArgumentException if validation fails
     */
    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD", justification = "Method is called from handle() method - SpotBugs false positive")
    private void validateQuery(ListStockMovementsQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (query.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
    }

    /**
     * Finds movement views based on query filters.
     *
     * @param query Query with filters
     * @return List of StockMovementViews
     */
    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD", justification = "Method is called from handle() method - SpotBugs false positive")
    private List<com.ccbsa.wms.location.application.service.port.data.dto.StockMovementView> findMovements(ListStockMovementsQuery query) {
        if (query.getStockItemId() != null && !query.getStockItemId().trim().isEmpty()) {
            return viewRepository.findByTenantIdAndStockItemId(query.getTenantId(), query.getStockItemId());
        } else if (query.getSourceLocationId() != null) {
            return viewRepository.findByTenantIdAndSourceLocationId(query.getTenantId(), query.getSourceLocationId());
        } else if (query.getDestinationLocationId() != null) {
            return viewRepository.findByTenantIdAndDestinationLocationId(query.getTenantId(), query.getDestinationLocationId());
        } else if (query.getStatus() != null) {
            return viewRepository.findByTenantIdAndStatus(query.getTenantId(), query.getStatus());
        } else {
            return viewRepository.findByTenantId(query.getTenantId());
        }
    }

    /**
     * Maps StockMovementView to StockMovementQueryResult DTO.
     *
     * @param view StockMovementView read model
     * @return StockMovementQueryResult DTO
     */
    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD", justification = "Method is called via method reference in stream (this::mapToQueryResult) - SpotBugs false positive")
    private StockMovementQueryResult mapToQueryResult(com.ccbsa.wms.location.application.service.port.data.dto.StockMovementView view) {
        return StockMovementQueryResult.builder().stockMovementId(view.getStockMovementId()).stockItemId(view.getStockItemId()).productId(view.getProductId())
                .sourceLocationId(view.getSourceLocationId()).destinationLocationId(view.getDestinationLocationId()).quantity(view.getQuantity())
                .movementType(view.getMovementType()).reason(view.getReason()).status(view.getStatus()).initiatedBy(view.getInitiatedBy()).initiatedAt(view.getInitiatedAt())
                .completedBy(view.getCompletedBy()).completedAt(view.getCompletedAt()).cancelledBy(view.getCancelledBy()).cancelledAt(view.getCancelledAt())
                .cancellationReason(view.getCancellationReason()).build();
    }
}

