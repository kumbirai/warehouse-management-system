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

/**
 * REST Controller: LocationQueryController
 * <p>
 * Handles location query operations (read operations).
 * <p>
 * Responsibilities: - Get location by ID endpoint - Map queries to DTOs - Return standardized API responses
 */
@RestController
@RequestMapping("/api/v1/location-management/locations")
@Tag(name = "Location Queries",
        description = "Location query operations")
public class LocationQueryController {
    private final GetLocationQueryHandler queryHandler;
    private final LocationDTOMapper mapper;

    public LocationQueryController(GetLocationQueryHandler queryHandler, LocationDTOMapper mapper) {
        this.queryHandler = queryHandler;
        this.mapper = mapper;
    }

    @GetMapping("/{locationId}")
    @Operation(summary = "Get Location by ID",
            description = "Retrieves a location by ID")
    @PreAuthorize("hasAnyRole('VIEWER', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'LOCATION_MANAGER', 'OPERATOR', 'PICKER', 'STOCK_CLERK')")
    public ResponseEntity<ApiResponse<LocationQueryResultDTO>> getLocation(
            @PathVariable String locationId,
            @RequestHeader("X-Tenant-Id") String tenantId) {
        // Map to query
        GetLocationQuery query = mapper.toGetLocationQuery(locationId, tenantId);

        // Execute query
        LocationQueryResult result = queryHandler.handle(query);

        // Map result to DTO
        LocationQueryResultDTO resultDTO = mapper.toQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }
}

