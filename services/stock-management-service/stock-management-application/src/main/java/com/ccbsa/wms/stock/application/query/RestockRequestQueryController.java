package com.ccbsa.wms.stock.application.query;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.wms.stock.application.dto.mapper.StockManagementDTOMapper;
import com.ccbsa.wms.stock.application.dto.query.ListRestockRequestsQueryResultDTO;
import com.ccbsa.wms.stock.application.service.query.ListRestockRequestsQueryHandler;
import com.ccbsa.wms.stock.application.service.query.dto.ListRestockRequestsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.ListRestockRequestsQueryResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller: RestockRequestQueryController
 * <p>
 * Handles restock request query operations (read operations).
 * <p>
 * Responsibilities:
 * - List restock requests endpoint
 * - Map queries to DTOs
 * - Return standardized API responses
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stock-management/restock-requests")
@Tag(name = "Restock Request Queries", description = "Restock request query operations")
@RequiredArgsConstructor
public class RestockRequestQueryController {
    private final ListRestockRequestsQueryHandler listRestockRequestsQueryHandler;
    private final StockManagementDTOMapper mapper;

    @GetMapping
    @Operation(summary = "List Restock Requests", description = "Retrieves a list of restock requests with optional filtering")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<ListRestockRequestsQueryResultDTO>> listRestockRequests(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                              @RequestParam(required = false) String status,
                                                                                              @RequestParam(required = false) String priority,
                                                                                              @RequestParam(required = false) String productId,
                                                                                              @RequestParam(value = "page", defaultValue = "0") Integer page,
                                                                                              @RequestParam(value = "size", defaultValue = "100") Integer size) {
        log.debug("Listing restock requests for tenant: {}, status: {}, priority: {}, productId: {}, page: {}, size: {}", tenantId, status, priority, productId, page, size);

        // Map to query
        ListRestockRequestsQuery query = mapper.toListRestockRequestsQuery(tenantId, status, priority, productId, page, size);

        // Execute query
        ListRestockRequestsQueryResult result = listRestockRequestsQueryHandler.handle(query);

        // Map result to DTO
        ListRestockRequestsQueryResultDTO resultDTO = mapper.toListRestockRequestsQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }
}
