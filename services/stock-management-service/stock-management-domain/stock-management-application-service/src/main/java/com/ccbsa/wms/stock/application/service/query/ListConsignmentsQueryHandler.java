package com.ccbsa.wms.stock.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.stock.application.service.port.data.StockConsignmentViewRepository;
import com.ccbsa.wms.stock.application.service.query.dto.ConsignmentQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.ListConsignmentsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.ListConsignmentsQueryResult;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;

/**
 * Query Handler: ListConsignmentsQueryHandler
 * <p>
 * Handles listing of StockConsignment read models with pagination.
 * <p>
 * Responsibilities:
 * - Load StockConsignment views from data port (read model)
 * - Apply pagination
 * - Map views to query result DTOs
 * - Return paginated results
 * <p>
 * Uses data port (StockConsignmentViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@RequiredArgsConstructor
public class ListConsignmentsQueryHandler {
    private final StockConsignmentViewRepository viewRepository;

    @Transactional(readOnly = true)
    public ListConsignmentsQueryResult handle(ListConsignmentsQuery query) {
        // 1. Normalize pagination parameters
        int page = query.getPage() != null ? query.getPage() : 0;
        int size = query.getSize() != null ? query.getSize() : 100;

        // 2. Query consignment views with pagination
        List<com.ccbsa.wms.stock.application.service.port.data.dto.StockConsignmentView> consignmentViews = viewRepository.findByTenantId(query.getTenantId(), page, size);

        // 3. Get total count for pagination metadata
        @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE", justification = "totalCount and consignmentResults are used in builder - SpotBugs false positive") long totalCount =
                viewRepository.countByTenantId(query.getTenantId());

        // 4. Map views to query results
        List<ConsignmentQueryResult> consignmentResults = consignmentViews.stream().map(this::toConsignmentQueryResult).collect(Collectors.toList());

        // 5. Build result with pagination metadata
        return ListConsignmentsQueryResult.builder().consignments(consignmentResults).totalCount((int) totalCount).page(page).size(size).build();
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD", justification = "Method is called via method reference in stream (this::toConsignmentQueryResult) - SpotBugs false"
            + " positive")
    private ConsignmentQueryResult toConsignmentQueryResult(com.ccbsa.wms.stock.application.service.port.data.dto.StockConsignmentView view) {
        return ConsignmentQueryResult.builder().consignmentId(view.getConsignmentId()).consignmentReference(view.getConsignmentReference()).warehouseId(view.getWarehouseId())
                .status(view.getStatus()).receivedAt(view.getReceivedAt()).confirmedAt(view.getConfirmedAt()).receivedBy(view.getReceivedBy()).lineItems(view.getLineItems())
                .createdAt(view.getCreatedAt()).lastModifiedAt(view.getLastModifiedAt()).build();
    }
}

