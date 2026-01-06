package com.ccbsa.wms.stock.application.query;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.wms.stock.application.dto.mapper.StockConsignmentDTOMapper;
import com.ccbsa.wms.stock.application.dto.query.ConsignmentQueryDTO;
import com.ccbsa.wms.stock.application.service.query.GetConsignmentQueryHandler;
import com.ccbsa.wms.stock.application.service.query.ListConsignmentsQueryHandler;
import com.ccbsa.wms.stock.application.service.query.dto.ConsignmentQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.GetConsignmentQuery;
import com.ccbsa.wms.stock.application.service.query.dto.ListConsignmentsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.ListConsignmentsQueryResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller: StockConsignmentQueryController
 * <p>
 * Handles stock consignment query operations (read operations).
 * <p>
 * Responsibilities: - Get consignment by ID endpoints - List consignments with pagination - Map queries to DTOs - Return standardized API responses
 */
@RestController
@RequestMapping("/api/v1/stock-management/consignments")
@Tag(name = "Stock Consignment Queries", description = "Stock consignment query operations")
public class StockConsignmentQueryController {
    private final GetConsignmentQueryHandler getConsignmentQueryHandler;
    private final ListConsignmentsQueryHandler listConsignmentsQueryHandler;
    private final StockConsignmentDTOMapper mapper;

    public StockConsignmentQueryController(GetConsignmentQueryHandler getConsignmentQueryHandler, ListConsignmentsQueryHandler listConsignmentsQueryHandler,
                                           StockConsignmentDTOMapper mapper) {
        this.getConsignmentQueryHandler = getConsignmentQueryHandler;
        this.listConsignmentsQueryHandler = listConsignmentsQueryHandler;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List Consignments", description = "Retrieves a paginated list of consignments")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<List<ConsignmentQueryDTO>>> listConsignments(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                   @RequestParam(value = "page", defaultValue = "0") Integer page,
                                                                                   @RequestParam(value = "size", defaultValue = "10") Integer size,
                                                                                   @RequestParam(value = "expiringWithinDays", required = false) Integer expiringWithinDays) {
        // Map to query
        ListConsignmentsQuery query = mapper.toListConsignmentsQuery(tenantId, page, size, expiringWithinDays);

        // Execute query
        ListConsignmentsQueryResult result = listConsignmentsQueryHandler.handle(query);

        // Map result to DTOs
        List<ConsignmentQueryDTO> resultDTOs = mapper.toConsignmentQueryDTOList(result, tenantId);

        return ApiResponseBuilder.ok(resultDTOs);
    }

    @GetMapping("/{consignmentId}")
    @Operation(summary = "Get Consignment", description = "Retrieves a consignment by ID")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<ConsignmentQueryDTO>> getConsignment(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable String consignmentId) {
        // Map to query
        GetConsignmentQuery query = mapper.toGetConsignmentQuery(consignmentId, tenantId);

        // Execute query
        ConsignmentQueryResult result = getConsignmentQueryHandler.handle(query);

        // Map result to DTO
        ConsignmentQueryDTO resultDTO = mapper.toQueryResultDTO(result, tenantId);

        return ApiResponseBuilder.ok(resultDTO);
    }
}

