package com.ccbsa.wms.stock.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.stock.application.service.port.data.StockAllocationViewRepository;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockAllocationView;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockAllocationQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.ListStockAllocationsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.ListStockAllocationsQueryResult;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;

/**
 * Query Handler: ListStockAllocationsQueryHandler
 * <p>
 * Handles listing of StockAllocation read models with pagination.
 * <p>
 * Responsibilities:
 * - Load StockAllocation views from data port (read model)
 * - Apply pagination
 * - Map views to query result DTOs
 * - Return paginated results
 * <p>
 * Uses data port (StockAllocationViewRepository) instead of repository port for CQRS compliance.
 * <p>
 * Note: Currently supports basic pagination. Advanced filtering (by product, location, reference ID, status) can be
 * added to the data port interface and adapter if needed in the future.
 */
@Component
@RequiredArgsConstructor
public class ListStockAllocationsQueryHandler {
    private final StockAllocationViewRepository viewRepository;

    @Transactional(readOnly = true)
    public ListStockAllocationsQueryResult handle(ListStockAllocationsQuery query) {
        // 1. Normalize pagination parameters
        int page = query.getPage() != null ? query.getPage() : 0;
        int size = query.getSize() != null ? query.getSize() : 100;

        // 2. Query allocation views with pagination
        List<StockAllocationView> allocationViews = viewRepository.findByTenantId(query.getTenantId(), page, size);

        // 3. Filter by status if provided (in-memory filtering for now)
        if (query.getStatus() != null) {
            allocationViews = allocationViews.stream().filter(view -> view.getStatus() == query.getStatus()).collect(Collectors.toList());
        }

        // 4. Get total count for pagination metadata
        @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE", justification = "totalCount and results are used in builder - SpotBugs false positive") long totalCount =
                viewRepository.countByTenantId(query.getTenantId());

        // 5. Map views to query results
        List<GetStockAllocationQueryResult> results = allocationViews.stream().map(this::mapToQueryResult).collect(Collectors.toList());

        // 6. Build result with pagination metadata
        return ListStockAllocationsQueryResult.builder().allocations(results).totalCount((int) totalCount).build();
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD", justification = "Method is called via method reference in stream (this::mapToQueryResult) - SpotBugs false positive")
    private GetStockAllocationQueryResult mapToQueryResult(StockAllocationView view) {
        return GetStockAllocationQueryResult.builder().allocationId(view.getAllocationId()).productId(view.getProductId()).locationId(view.getLocationId())
                .stockItemId(view.getStockItemId()).quantity(view.getQuantity()).allocationType(view.getAllocationType()).referenceId(view.getReferenceId())
                .status(view.getStatus()).allocatedAt(view.getAllocatedAt()).releasedAt(view.getReleasedAt()).allocatedBy(view.getAllocatedBy()).notes(view.getNotes()).build();
    }
}

