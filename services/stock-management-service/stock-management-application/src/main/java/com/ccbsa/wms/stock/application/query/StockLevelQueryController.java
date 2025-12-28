package com.ccbsa.wms.stock.application.query;

import java.util.List;

import org.springframework.http.HttpStatus;
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
import com.ccbsa.wms.stock.application.dto.query.StockLevelQueryDTO;
import com.ccbsa.wms.stock.application.service.query.GetStockLevelsQueryHandler;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockLevelsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockLevelsQueryResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller: StockLevelQueryController
 * <p>
 * Handles stock level query operations (read operations).
 * <p>
 * Responsibilities:
 * - Get stock levels by product and optionally location
 * - Aggregate stock items and allocations to calculate stock levels
 * - Map query results to DTOs
 * - Return standardized API responses
 */
@RestController
@RequestMapping("/api/v1/stock-management/stock-levels")
@Tag(name = "Stock Level Queries", description = "Stock level query operations")
@Slf4j
public class StockLevelQueryController {
    private final GetStockLevelsQueryHandler getStockLevelsQueryHandler;
    private final StockManagementDTOMapper mapper;

    public StockLevelQueryController(GetStockLevelsQueryHandler getStockLevelsQueryHandler, StockManagementDTOMapper mapper) {
        this.getStockLevelsQueryHandler = getStockLevelsQueryHandler;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Get Stock Levels", description = "Retrieves stock levels by product and optionally location")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<StockLevelQueryDTO>>> getStockLevels(@RequestHeader("X-Tenant-Id") String tenantId, @RequestParam("productId") String productId,
                                                                                @RequestParam(required = false) String locationId) {
        try {
            // Map to query
            GetStockLevelsQuery query = mapper.toGetStockLevelsQuery(tenantId, productId, locationId);

            // Execute query
            GetStockLevelsQueryResult result = getStockLevelsQueryHandler.handle(query);

            // Map result to DTOs
            List<StockLevelQueryDTO> resultDTOs = mapper.toStockLevelQueryDTOList(result);

            return ApiResponseBuilder.ok(resultDTOs);
        } catch (IllegalArgumentException e) {
            // Handle validation errors (e.g., invalid UUID format)
            log.warn("Invalid request parameters for stock levels query: {}", e.getMessage());
            return ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request parameters: " + e.getMessage());
        } catch (Exception e) {
            // Handle unexpected errors - log and return generic error
            log.error("Error retrieving stock levels", e);
            return ApiResponseBuilder.error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred while retrieving stock levels");
        }
    }
}
