package com.ccbsa.wms.location.application.query;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.wms.location.application.dto.mapper.LocationDTOMapper;
import com.ccbsa.wms.location.application.dto.query.LocationQueryResultDTO;
import com.ccbsa.wms.location.application.service.query.GetLocationQueryHandler;
import com.ccbsa.wms.location.application.service.query.dto.GetLocationQuery;
import com.ccbsa.wms.location.application.service.query.dto.LocationQueryResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller: WarehouseQueryController
 * <p>
 * Handles warehouse query operations. Warehouses are locations with type "WAREHOUSE".
 * <p>
 * This controller provides warehouse-specific endpoints that delegate to location queries.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/location-management/warehouses")
@Tag(name = "Warehouse Queries", description = "Warehouse query operations")
@RequiredArgsConstructor
public class WarehouseQueryController {
    private final GetLocationQueryHandler getLocationQueryHandler;
    private final LocationDTOMapper mapper;

    @GetMapping("/{warehouseId}")
    @Operation(summary = "Get Warehouse by ID", description = "Retrieves a warehouse by ID. Warehouses are locations with type WAREHOUSE.")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'LOCATION_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<LocationQueryResultDTO>> getWarehouse(@PathVariable String warehouseId, @RequestHeader("X-Tenant-Id") String tenantId) {
        // Map to location query (warehouses are locations)
        GetLocationQuery query = mapper.toGetLocationQuery(warehouseId, tenantId);

        // Execute query
        LocationQueryResult result = getLocationQueryHandler.handle(query);

        // Verify this is actually a warehouse
        if (result.getType() == null || !"WAREHOUSE".equalsIgnoreCase(result.getType().trim())) {
            log.warn("Location {} is not a warehouse (type: {}). Returning location details anyway.", warehouseId, result.getType());
        }

        // Map result to DTO
        LocationQueryResultDTO resultDTO = mapper.toQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }
}
