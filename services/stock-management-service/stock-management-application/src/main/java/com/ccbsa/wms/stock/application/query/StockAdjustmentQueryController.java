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
import com.ccbsa.wms.stock.application.dto.query.ListStockAdjustmentsQueryResultDTO;
import com.ccbsa.wms.stock.application.dto.query.StockAdjustmentQueryDTO;
import com.ccbsa.wms.stock.application.service.query.GetStockAdjustmentQueryHandler;
import com.ccbsa.wms.stock.application.service.query.ListStockAdjustmentsQueryHandler;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockAdjustmentQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockAdjustmentQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.ListStockAdjustmentsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.ListStockAdjustmentsQueryResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller: StockAdjustmentQueryController
 * <p>
 * Handles stock adjustment query operations (read operations).
 * <p>
 * Responsibilities:
 * - Get stock adjustment by ID endpoint
 * - List stock adjustments endpoint
 * - Map queries to DTOs
 * - Return standardized API responses
 */
@RestController
@RequestMapping("/api/v1/stock-management/adjustments")
@Tag(name = "Stock Adjustment Queries", description = "Stock adjustment query operations")
public class StockAdjustmentQueryController {
    private final GetStockAdjustmentQueryHandler getStockAdjustmentQueryHandler;
    private final ListStockAdjustmentsQueryHandler listStockAdjustmentsQueryHandler;
    private final StockManagementDTOMapper mapper;

    public StockAdjustmentQueryController(GetStockAdjustmentQueryHandler getStockAdjustmentQueryHandler, ListStockAdjustmentsQueryHandler listStockAdjustmentsQueryHandler,
                                          StockManagementDTOMapper mapper) {
        this.getStockAdjustmentQueryHandler = getStockAdjustmentQueryHandler;
        this.listStockAdjustmentsQueryHandler = listStockAdjustmentsQueryHandler;
        this.mapper = mapper;
    }

    @GetMapping("/{adjustmentId}")
    @Operation(summary = "Get Stock Adjustment by ID", description = "Retrieves a stock adjustment by ID")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<StockAdjustmentQueryDTO>> getStockAdjustment(@PathVariable String adjustmentId, @RequestHeader("X-Tenant-Id") String tenantId) {
        // Map to query
        GetStockAdjustmentQuery query = mapper.toGetStockAdjustmentQuery(adjustmentId, tenantId);

        // Execute query
        GetStockAdjustmentQueryResult result = getStockAdjustmentQueryHandler.handle(query);

        // Map result to DTO
        StockAdjustmentQueryDTO resultDTO = mapper.toStockAdjustmentQueryDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }

    @GetMapping
    @Operation(summary = "List Stock Adjustments", description = "Retrieves a list of stock adjustments with optional filtering")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<ListStockAdjustmentsQueryResultDTO>> listStockAdjustments(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                                @RequestParam(required = false) String productId,
                                                                                                @RequestParam(required = false) String locationId,
                                                                                                @RequestParam(required = false) String stockItemId,
                                                                                                @RequestParam(value = "page", defaultValue = "0") Integer page,
                                                                                                @RequestParam(value = "size", defaultValue = "10") Integer size) {
        // Map to query
        ListStockAdjustmentsQuery query = mapper.toListStockAdjustmentsQuery(tenantId, productId, locationId, stockItemId, page, size);

        // Execute query
        ListStockAdjustmentsQueryResult result = listStockAdjustmentsQueryHandler.handle(query);

        // Map result to DTO
        ListStockAdjustmentsQueryResultDTO resultDTO = mapper.toListStockAdjustmentsQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }
}

