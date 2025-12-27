package com.ccbsa.wms.stock.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.stock.application.service.port.repository.StockConsignmentRepository;
import com.ccbsa.wms.stock.application.service.query.dto.ConsignmentQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.ListConsignmentsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.ListConsignmentsQueryResult;
import com.ccbsa.wms.stock.domain.core.entity.StockConsignment;

/**
 * Query Handler: ListConsignmentsQueryHandler
 * <p>
 * Handles listing of StockConsignment aggregates with pagination.
 * <p>
 * Responsibilities:
 * - Load StockConsignment aggregates from repository
 * - Apply pagination
 * - Map aggregates to query result DTOs
 * - Return paginated results
 */
@Component
public class ListConsignmentsQueryHandler {
    private final StockConsignmentRepository repository;

    public ListConsignmentsQueryHandler(StockConsignmentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ListConsignmentsQueryResult handle(ListConsignmentsQuery query) {
        // 1. Normalize pagination parameters
        int page = query.getPage() != null ? query.getPage() : 0;
        int size = query.getSize() != null ? query.getSize() : 100;

        // 2. Query consignments with pagination
        List<StockConsignment> consignments = repository.findByTenantId(query.getTenantId(), page, size);

        // 3. Get total count for pagination metadata
        long totalCount = repository.countByTenantId(query.getTenantId());

        // 4. Map to query results
        List<ConsignmentQueryResult> consignmentResults = consignments.stream().map(this::toConsignmentQueryResult).collect(Collectors.toList());

        // 5. Build result with pagination metadata
        return ListConsignmentsQueryResult.builder().consignments(consignmentResults).totalCount((int) totalCount).page(page).size(size).build();
    }

    private ConsignmentQueryResult toConsignmentQueryResult(StockConsignment consignment) {
        return ConsignmentQueryResult.builder().consignmentId(consignment.getId()).consignmentReference(consignment.getConsignmentReference())
                .warehouseId(consignment.getWarehouseId()).status(consignment.getStatus()).receivedAt(consignment.getReceivedAt()).confirmedAt(consignment.getConfirmedAt())
                .receivedBy(consignment.getReceivedBy()).lineItems(consignment.getLineItems()).createdAt(consignment.getCreatedAt()).lastModifiedAt(consignment.getLastModifiedAt())
                .build();
    }
}

