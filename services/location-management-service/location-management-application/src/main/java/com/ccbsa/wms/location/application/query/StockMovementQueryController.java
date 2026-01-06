package com.ccbsa.wms.location.application.query;

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
import com.ccbsa.wms.location.application.dto.mapper.LocationDTOMapper;
import com.ccbsa.wms.location.application.dto.query.ListStockMovementsQueryResultDTO;
import com.ccbsa.wms.location.application.dto.query.StockMovementQueryResultDTO;
import com.ccbsa.wms.location.application.service.query.GetStockMovementQueryHandler;
import com.ccbsa.wms.location.application.service.query.ListStockMovementsQueryHandler;
import com.ccbsa.wms.location.application.service.query.dto.GetStockMovementQuery;
import com.ccbsa.wms.location.application.service.query.dto.ListStockMovementsQuery;
import com.ccbsa.wms.location.application.service.query.dto.ListStockMovementsQueryResult;
import com.ccbsa.wms.location.application.service.query.dto.StockMovementQueryResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller: StockMovementQueryController
 * <p>
 * Handles stock movement query operations (read operations).
 * <p>
 * Responsibilities:
 * - Get stock movement by ID endpoint
 * - List stock movements endpoint
 * - Map queries to DTOs
 * - Return standardized API responses
 */
@RestController
@RequestMapping("/api/v1/location-management/stock-movements")
@Tag(name = "Stock Movement Queries", description = "Stock movement query operations")
public class StockMovementQueryController {
    private final GetStockMovementQueryHandler getStockMovementQueryHandler;
    private final ListStockMovementsQueryHandler listStockMovementsQueryHandler;
    private final LocationDTOMapper mapper;

    public StockMovementQueryController(GetStockMovementQueryHandler getStockMovementQueryHandler, ListStockMovementsQueryHandler listStockMovementsQueryHandler,
                                        LocationDTOMapper mapper) {
        this.getStockMovementQueryHandler = getStockMovementQueryHandler;
        this.listStockMovementsQueryHandler = listStockMovementsQueryHandler;
        this.mapper = mapper;
    }

    @GetMapping("/{movementId}")
    @Operation(summary = "Get Stock Movement by ID", description = "Retrieves a stock movement by ID")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'LOCATION_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<StockMovementQueryResultDTO>> getStockMovement(@PathVariable String movementId, @RequestHeader("X-Tenant-Id") String tenantId) {
        // Map to query
        GetStockMovementQuery query = mapper.toGetStockMovementQuery(movementId, tenantId);

        // Execute query
        StockMovementQueryResult result = getStockMovementQueryHandler.handle(query);

        // Map result to DTO
        StockMovementQueryResultDTO resultDTO = mapper.toStockMovementQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }

    @GetMapping
    @Operation(summary = "List Stock Movements", description = "Retrieves a list of stock movements with optional filtering")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'LOCATION_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<ListStockMovementsQueryResultDTO>> listStockMovements(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                            @RequestParam(required = false) String stockItemId,
                                                                                            @RequestParam(required = false) String sourceLocationId) {
        // Map to query
        ListStockMovementsQuery query = mapper.toListStockMovementsQuery(tenantId, stockItemId, sourceLocationId);

        // Execute query
        ListStockMovementsQueryResult result = listStockMovementsQueryHandler.handle(query);

        // Map result to DTO
        ListStockMovementsQueryResultDTO resultDTO = mapper.toListStockMovementsQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }
}

