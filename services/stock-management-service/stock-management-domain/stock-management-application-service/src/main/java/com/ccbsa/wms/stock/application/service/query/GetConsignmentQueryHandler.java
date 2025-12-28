package com.ccbsa.wms.stock.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.stock.application.service.port.data.StockConsignmentViewRepository;
import com.ccbsa.wms.stock.application.service.query.dto.ConsignmentQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.GetConsignmentQuery;
import com.ccbsa.wms.stock.domain.core.exception.ConsignmentNotFoundException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;

/**
 * Query Handler: GetConsignmentQueryHandler
 * <p>
 * Handles retrieval of StockConsignment read model by ID.
 * <p>
 * Responsibilities:
 * - Load StockConsignment view from data port (read model)
 * - Map view to query result DTO
 * - Return optimized read model
 * <p>
 * Uses data port (StockConsignmentViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@RequiredArgsConstructor
public class GetConsignmentQueryHandler {
    private final StockConsignmentViewRepository viewRepository;

    @Transactional(readOnly = true)
    @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE", justification = "consignmentView is used in builder chain - SpotBugs false positive with var keyword")
    public ConsignmentQueryResult handle(GetConsignmentQuery query) {
        // 1. Load read model (view) from data port
        var consignmentView = viewRepository.findByTenantIdAndId(query.getTenantId(), query.getConsignmentId())
                .orElseThrow(() -> new ConsignmentNotFoundException(String.format("Consignment not found: %s", query.getConsignmentId().getValueAsString())));

        // 2. Map view to query result
        return ConsignmentQueryResult.builder().consignmentId(consignmentView.getConsignmentId()).consignmentReference(consignmentView.getConsignmentReference())
                .warehouseId(consignmentView.getWarehouseId()).status(consignmentView.getStatus()).receivedAt(consignmentView.getReceivedAt())
                .confirmedAt(consignmentView.getConfirmedAt()).receivedBy(consignmentView.getReceivedBy()).lineItems(consignmentView.getLineItems())
                .createdAt(consignmentView.getCreatedAt()).lastModifiedAt(consignmentView.getLastModifiedAt()).build();
    }
}

