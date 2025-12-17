package com.ccbsa.wms.stockmanagement.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.stockmanagement.application.service.port.repository.StockConsignmentRepository;
import com.ccbsa.wms.stockmanagement.application.service.query.dto.ConsignmentQueryResult;
import com.ccbsa.wms.stockmanagement.application.service.query.dto.GetConsignmentQuery;
import com.ccbsa.wms.stockmanagement.domain.core.entity.StockConsignment;
import com.ccbsa.wms.stockmanagement.domain.core.exception.ConsignmentNotFoundException;

/**
 * Query Handler: GetConsignmentQueryHandler
 * <p>
 * Handles retrieval of StockConsignment aggregate by ID.
 * <p>
 * Responsibilities: - Load StockConsignment aggregate from repository - Map aggregate to query result DTO - Return optimized read model
 */
@Component
public class GetConsignmentQueryHandler {
    private final StockConsignmentRepository repository;

    public GetConsignmentQueryHandler(StockConsignmentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ConsignmentQueryResult handle(GetConsignmentQuery query) {
        // 1. Load aggregate
        StockConsignment consignment = repository.findByIdAndTenantId(query.getConsignmentId(), query.getTenantId())
                .orElseThrow(() -> new ConsignmentNotFoundException(String.format("Consignment not found: %s", query.getConsignmentId()
                        .getValueAsString())));

        // 2. Map to query result
        return ConsignmentQueryResult.builder()
                .consignmentId(consignment.getId())
                .consignmentReference(consignment.getConsignmentReference())
                .warehouseId(consignment.getWarehouseId())
                .status(consignment.getStatus())
                .receivedAt(consignment.getReceivedAt())
                .confirmedAt(consignment.getConfirmedAt())
                .receivedBy(consignment.getReceivedBy())
                .lineItems(consignment.getLineItems())
                .createdAt(consignment.getCreatedAt())
                .lastModifiedAt(consignment.getLastModifiedAt())
                .build();
    }
}

