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
import com.ccbsa.wms.location.application.dto.query.ListLocationsQueryResultDTO;
import com.ccbsa.wms.location.application.dto.query.LocationQueryResultDTO;
import com.ccbsa.wms.location.application.service.query.GetLocationQueryHandler;
import com.ccbsa.wms.location.application.service.query.ListLocationsQueryHandler;
import com.ccbsa.wms.location.application.service.query.dto.GetLocationQuery;
import com.ccbsa.wms.location.application.service.query.dto.ListLocationsQuery;
import com.ccbsa.wms.location.application.service.query.dto.ListLocationsQueryResult;
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
@RequestMapping("/locations")
@Tag(name = "Location Queries",
        description = "Location query operations")
public class LocationQueryController {
    private final GetLocationQueryHandler getLocationQueryHandler;
    private final ListLocationsQueryHandler listLocationsQueryHandler;
    private final LocationDTOMapper mapper;

    public LocationQueryController(GetLocationQueryHandler getLocationQueryHandler,
                                   ListLocationsQueryHandler listLocationsQueryHandler,
                                   LocationDTOMapper mapper) {
        this.getLocationQueryHandler = getLocationQueryHandler;
        this.listLocationsQueryHandler = listLocationsQueryHandler;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List Locations",
            description = "Retrieves a list of locations with optional filtering and pagination")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'LOCATION_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'VIEWER')")
    public ResponseEntity<ApiResponse<ListLocationsQueryResultDTO>> listLocations(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        // Map to query
        ListLocationsQuery query = mapper.toListLocationsQuery(tenantId, page, size, zone, status, search);

        // Execute query
        ListLocationsQueryResult result = listLocationsQueryHandler.handle(query);

        // Map result to DTO
        ListLocationsQueryResultDTO resultDTO = mapper.toListLocationsQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }

    @GetMapping("/{locationId}")
    @Operation(summary = "Get Location by ID",
            description = "Retrieves a location by ID")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'LOCATION_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'VIEWER')")
    public ResponseEntity<ApiResponse<LocationQueryResultDTO>> getLocation(
            @PathVariable String locationId,
            @RequestHeader("X-Tenant-Id") String tenantId) {
        // Map to query
        GetLocationQuery query = mapper.toGetLocationQuery(locationId, tenantId);

        // Execute query
        LocationQueryResult result = getLocationQueryHandler.handle(query);

        // Map result to DTO
        LocationQueryResultDTO resultDTO = mapper.toQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }
}

