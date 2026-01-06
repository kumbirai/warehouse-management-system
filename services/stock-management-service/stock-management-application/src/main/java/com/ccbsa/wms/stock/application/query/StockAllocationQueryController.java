package com.ccbsa.wms.stock.application.query;

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
import com.ccbsa.wms.stock.application.dto.mapper.StockManagementDTOMapper;
import com.ccbsa.wms.stock.application.dto.query.ListStockAllocationsQueryResultDTO;
import com.ccbsa.wms.stock.application.dto.query.StockAllocationQueryDTO;
import com.ccbsa.wms.stock.application.service.query.GetStockAllocationQueryHandler;
import com.ccbsa.wms.stock.application.service.query.ListStockAllocationsQueryHandler;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockAllocationQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockAllocationQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.ListStockAllocationsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.ListStockAllocationsQueryResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller: StockAllocationQueryController
 * <p>
 * Handles stock allocation query operations (read operations).
 * <p>
 * Responsibilities:
 * - Get stock allocation by ID endpoint
 * - List stock allocations endpoint
 * - Map queries to DTOs
 * - Return standardized API responses
 */
@RestController
@RequestMapping("/api/v1/stock-management/allocations")
@Tag(name = "Stock Allocation Queries", description = "Stock allocation query operations")
public class StockAllocationQueryController {
    private final GetStockAllocationQueryHandler getStockAllocationQueryHandler;
    private final ListStockAllocationsQueryHandler listStockAllocationsQueryHandler;
    private final StockManagementDTOMapper mapper;

    public StockAllocationQueryController(GetStockAllocationQueryHandler getStockAllocationQueryHandler, ListStockAllocationsQueryHandler listStockAllocationsQueryHandler,
                                          StockManagementDTOMapper mapper) {
        this.getStockAllocationQueryHandler = getStockAllocationQueryHandler;
        this.listStockAllocationsQueryHandler = listStockAllocationsQueryHandler;
        this.mapper = mapper;
    }

    @GetMapping("/{allocationId}")
    @Operation(summary = "Get Stock Allocation by ID", description = "Retrieves a stock allocation by ID")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<StockAllocationQueryDTO>> getStockAllocation(@PathVariable String allocationId, @RequestHeader("X-Tenant-Id") String tenantId) {
        // Map to query
        GetStockAllocationQuery query = mapper.toGetStockAllocationQuery(allocationId, tenantId);

        // Execute query
        GetStockAllocationQueryResult result = getStockAllocationQueryHandler.handle(query);

        // Map result to DTO
        StockAllocationQueryDTO resultDTO = mapper.toStockAllocationQueryDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }

    @GetMapping
    @Operation(summary = "List Stock Allocations", description = "Retrieves a list of stock allocations with optional filtering")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<ListStockAllocationsQueryResultDTO>> listStockAllocations(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                                @RequestParam(required = false) String productId,
                                                                                                @RequestParam(required = false) String locationId,
                                                                                                @RequestParam(required = false) String referenceId,
                                                                                                @RequestParam(required = false) String status,
                                                                                                @RequestParam(value = "page", defaultValue = "0") Integer page,
                                                                                                @RequestParam(value = "size", defaultValue = "10") Integer size) {
        // Map to query
        ListStockAllocationsQuery query = mapper.toListStockAllocationsQuery(tenantId, productId, locationId, referenceId, status, page, size);

        // Execute query
        ListStockAllocationsQueryResult result = listStockAllocationsQueryHandler.handle(query);

        // Map result to DTO
        ListStockAllocationsQueryResultDTO resultDTO = mapper.toListStockAllocationsQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }
}

