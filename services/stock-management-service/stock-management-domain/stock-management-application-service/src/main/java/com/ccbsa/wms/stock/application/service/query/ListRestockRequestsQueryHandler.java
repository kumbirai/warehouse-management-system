package com.ccbsa.wms.stock.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.wms.stock.application.service.port.repository.RestockRequestRepository;
import com.ccbsa.wms.stock.application.service.query.dto.ListRestockRequestsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.ListRestockRequestsQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.RestockRequestQueryResult;
import com.ccbsa.wms.stock.domain.core.entity.RestockRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: ListRestockRequestsQueryHandler
 * <p>
 * Handles listing of RestockRequest aggregates with filtering.
 * <p>
 * Responsibilities:
 * - Load restock requests from repository
 * - Apply filters (status, priority, productId)
 * - Map aggregates to query result DTOs
 * - Return results
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ListRestockRequestsQueryHandler {
    private final RestockRequestRepository repository;

    @Transactional(readOnly = true)
    public ListRestockRequestsQueryResult handle(ListRestockRequestsQuery query) {
        log.debug("Handling ListRestockRequestsQuery for tenant: {}, status: {}, priority: {}, productId: {}", query.getTenantId().getValue(), query.getStatus(),
                query.getPriority(), query.getProductId());

        // 1. Query restock requests from repository
        List<RestockRequest> restockRequests = repository.findByTenantId(query.getTenantId(), query.getStatus());

        // 2. Apply additional filters (priority, productId) in memory
        List<RestockRequest> filtered = restockRequests.stream().filter(request -> {
            if (query.getPriority() != null && request.getPriority() != query.getPriority()) {
                return false;
            }
            if (query.getProductId() != null) {
                ProductId productId = ProductId.of(query.getProductId());
                if (!request.getProductId().equals(productId)) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());

        // 3. Apply pagination (simple in-memory pagination for now)
        int page = query.getPage() != null ? query.getPage() : 0;
        int size = query.getSize() != null ? query.getSize() : 100;
        int start = page * size;
        int end = Math.min(start + size, filtered.size());
        List<RestockRequest> paginated = start < filtered.size() ? filtered.subList(start, end) : List.of();

        // 4. Map to query results
        List<RestockRequestQueryResult> results = paginated.stream().map(this::mapToQueryResult).collect(Collectors.toList());

        // 5. Build result with defensive copy of list
        return ListRestockRequestsQueryResult.builder().requests(new java.util.ArrayList<>(results)).totalCount(filtered.size()).build();
    }

    private RestockRequestQueryResult mapToQueryResult(RestockRequest request) {
        return RestockRequestQueryResult.builder().restockRequestId(request.getId()).productId(request.getProductId()).locationId(request.getLocationId())
                .currentQuantity(request.getCurrentQuantity().getValue()).minimumQuantity(request.getMinimumQuantity().getValue())
                .maximumQuantity(request.getMaximumQuantity() != null ? request.getMaximumQuantity().getValue() : null).requestedQuantity(request.getRequestedQuantity().getValue())
                .priority(request.getPriority()).status(request.getStatus()).createdAt(request.getCreatedAt()).sentToD365At(request.getSentToD365At())
                .d365OrderReference(request.getD365OrderReference()).build();
    }
}
