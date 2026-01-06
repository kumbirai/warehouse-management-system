package com.ccbsa.wms.stock.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.stock.application.service.port.data.StockAdjustmentViewRepository;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockAdjustmentView;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockAdjustmentQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.ListStockAdjustmentsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.ListStockAdjustmentsQueryResult;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;

/**
 * Query Handler: ListStockAdjustmentsQueryHandler
 * <p>
 * Handles listing of StockAdjustment read models with pagination.
 * <p>
 * Responsibilities:
 * - Load StockAdjustment views from data port (read model)
 * - Apply pagination
 * - Map views to query result DTOs
 * - Return paginated results
 * <p>
 * Uses data port (StockAdjustmentViewRepository) instead of repository port for CQRS compliance.
 * <p>
 * Note: Currently supports basic pagination. Advanced filtering (by product, location, stock item) can be
 * added to the data port interface and adapter if needed in the future.
 */
@Component
@RequiredArgsConstructor
public class ListStockAdjustmentsQueryHandler {
    private final StockAdjustmentViewRepository viewRepository;

    @Transactional(readOnly = true)
    public ListStockAdjustmentsQueryResult handle(ListStockAdjustmentsQuery query) {
        // 1. Normalize pagination parameters
        int page = query.getPage() != null ? query.getPage() : 0;
        int size = query.getSize() != null ? query.getSize() : 100;

        // 2. Query adjustment views with pagination
        List<StockAdjustmentView> adjustmentViews = viewRepository.findByTenantId(query.getTenantId(), page, size);

        // 3. Get total count for pagination metadata
        @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE", justification = "totalCount and results are used in builder - SpotBugs false positive") long totalCount =
                viewRepository.countByTenantId(query.getTenantId());

        // 4. Map views to query results
        List<GetStockAdjustmentQueryResult> results = adjustmentViews.stream().map(this::mapToQueryResult).collect(Collectors.toList());

        // 5. Build result with pagination metadata
        return ListStockAdjustmentsQueryResult.builder().adjustments(results).totalCount((int) totalCount).build();
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD", justification = "Method is called via method reference in stream (this::mapToQueryResult) - SpotBugs false positive")
    private GetStockAdjustmentQueryResult mapToQueryResult(StockAdjustmentView view) {
        return GetStockAdjustmentQueryResult.builder().adjustmentId(view.getAdjustmentId()).productId(view.getProductId()).locationId(view.getLocationId())
                .stockItemId(view.getStockItemId()).adjustmentType(view.getAdjustmentType()).quantity(view.getQuantity()).quantityBefore(view.getQuantityBefore())
                .quantityAfter(view.getQuantityAfter()).reason(view.getReason()).notes(view.getNotes()).adjustedBy(view.getAdjustedBy())
                .authorizationCode(view.getAuthorizationCode()).adjustedAt(view.getAdjustedAt()).build();
    }
}

