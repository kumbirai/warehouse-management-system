package com.ccbsa.wms.returns.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.valueobject.ReturnStatus;
import com.ccbsa.wms.returns.application.service.port.repository.ReturnRepository;
import com.ccbsa.wms.returns.application.service.query.dto.GetReturnQueryResult;
import com.ccbsa.wms.returns.application.service.query.dto.ListReturnsQuery;
import com.ccbsa.wms.returns.application.service.query.dto.ListReturnsQueryResult;
import com.ccbsa.wms.returns.domain.core.entity.Return;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: ListReturnsQueryHandler
 * <p>
 * Handles retrieval of returns list with optional status filter and pagination.
 * <p>
 * Responsibilities:
 * - Load returns from repository (filtered by status if provided)
 * - Apply pagination
 * - Map aggregates to query result DTOs
 * - Return optimized read model
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ListReturnsQueryHandler {
    private final ReturnRepository returnRepository;
    private final GetReturnQueryHandler getReturnQueryHandler;

    @Transactional(readOnly = true)
    public ListReturnsQueryResult handle(ListReturnsQuery query) {
        log.debug("Handling ListReturnsQuery for tenant: {}, status: {}, page: {}, size: {}", query.getTenantId().getValue(), query.getStatus(), query.getPage(), query.getSize());

        // Load returns from repository
        List<Return> returns;
        if (query.getStatus() != null) {
            returns = returnRepository.findByStatusAndTenantId(query.getStatus(), query.getTenantId());
        } else {
            // If no status filter, get all statuses and combine
            // Note: This is a workaround - ideally repository would have findAllByTenantId
            List<Return> allReturns = new java.util.ArrayList<>();
            for (ReturnStatus status : ReturnStatus.values()) {
                allReturns.addAll(returnRepository.findByStatusAndTenantId(status, query.getTenantId()));
            }
            returns = allReturns;
        }

        // Apply pagination
        int start = query.getPage() * query.getSize();
        int end = Math.min(start + query.getSize(), returns.size());
        List<Return> paginatedReturns = returns.subList(Math.min(start, returns.size()), end);

        // Map to query results
        List<GetReturnQueryResult> returnResults = paginatedReturns.stream()
                .map(returnAggregate -> getReturnQueryHandler.mapToQueryResult(returnAggregate))
                .collect(Collectors.toList());

        // Calculate pagination metadata
        long totalElements = returns.size();
        int totalPages = (int) Math.ceil((double) totalElements / query.getSize());

        return ListReturnsQueryResult.builder()
                .returns(returnResults)
                .page(query.getPage())
                .size(query.getSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }
}
