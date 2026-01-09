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
import com.ccbsa.wms.location.application.dto.query.LocationHierarchyQueryResultDTO;
import com.ccbsa.wms.location.application.service.query.ListAislesQueryHandler;
import com.ccbsa.wms.location.application.service.query.ListBinsQueryHandler;
import com.ccbsa.wms.location.application.service.query.ListRacksQueryHandler;
import com.ccbsa.wms.location.application.service.query.ListWarehousesQueryHandler;
import com.ccbsa.wms.location.application.service.query.ListZonesQueryHandler;
import com.ccbsa.wms.location.application.service.query.dto.LocationHierarchyQueryResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller: LocationHierarchyQueryController
 * <p>
 * Handles hierarchical location query operations (warehouse -> zone -> aisle -> rack -> bin).
 * <p>
 * Responsibilities:
 * - List warehouses
 * - List zones under a warehouse
 * - List aisles under a zone
 * - List racks under an aisle
 * - List bins under a rack
 */
@RestController
@RequestMapping("/api/v1/location-management/locations")
@Tag(name = "Location Hierarchy Queries", description = "Hierarchical location query operations")
@RequiredArgsConstructor
public class LocationHierarchyQueryController {
    private final ListWarehousesQueryHandler listWarehousesQueryHandler;
    private final ListZonesQueryHandler listZonesQueryHandler;
    private final ListAislesQueryHandler listAislesQueryHandler;
    private final ListRacksQueryHandler listRacksQueryHandler;
    private final ListBinsQueryHandler listBinsQueryHandler;
    private final LocationDTOMapper mapper;

    @GetMapping("/warehouses")
    @Operation(summary = "List Warehouses", description = "Retrieves a list of all warehouses (locations with type WAREHOUSE)")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'LOCATION_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<LocationHierarchyQueryResultDTO>> listWarehouses(@RequestHeader("X-Tenant-Id") String tenantId) {
        // Map to query
        var query = mapper.toListWarehousesQuery(tenantId);

        // Execute query
        LocationHierarchyQueryResult result = listWarehousesQueryHandler.handle(query);

        // Map result to DTO
        LocationHierarchyQueryResultDTO resultDTO = mapper.toLocationHierarchyQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }

    @GetMapping("/warehouses/{warehouseId}/zones")
    @Operation(summary = "List Zones in Warehouse", description = "Retrieves a list of zones under a warehouse")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'LOCATION_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<LocationHierarchyQueryResultDTO>> listZones(@PathVariable String warehouseId, @RequestHeader("X-Tenant-Id") String tenantId) {
        // Map to query
        var query = mapper.toListZonesQuery(warehouseId, tenantId);

        // Execute query
        LocationHierarchyQueryResult result = listZonesQueryHandler.handle(query);

        // Map result to DTO
        LocationHierarchyQueryResultDTO resultDTO = mapper.toLocationHierarchyQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }

    @GetMapping("/zones/{zoneId}/aisles")
    @Operation(summary = "List Aisles in Zone", description = "Retrieves a list of aisles under a zone")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'LOCATION_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<LocationHierarchyQueryResultDTO>> listAisles(@PathVariable String zoneId, @RequestHeader("X-Tenant-Id") String tenantId) {
        // Map to query
        var query = mapper.toListAislesQuery(zoneId, tenantId);

        // Execute query
        LocationHierarchyQueryResult result = listAislesQueryHandler.handle(query);

        // Map result to DTO
        LocationHierarchyQueryResultDTO resultDTO = mapper.toLocationHierarchyQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }

    @GetMapping("/aisles/{aisleId}/racks")
    @Operation(summary = "List Racks in Aisle", description = "Retrieves a list of racks under an aisle")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'LOCATION_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<LocationHierarchyQueryResultDTO>> listRacks(@PathVariable String aisleId, @RequestHeader("X-Tenant-Id") String tenantId) {
        // Map to query
        var query = mapper.toListRacksQuery(aisleId, tenantId);

        // Execute query
        LocationHierarchyQueryResult result = listRacksQueryHandler.handle(query);

        // Map result to DTO
        LocationHierarchyQueryResultDTO resultDTO = mapper.toLocationHierarchyQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }

    @GetMapping("/racks/{rackId}/bins")
    @Operation(summary = "List Bins in Rack", description = "Retrieves a list of bins under a rack")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'LOCATION_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<LocationHierarchyQueryResultDTO>> listBins(@PathVariable String rackId, @RequestHeader("X-Tenant-Id") String tenantId) {
        // Map to query
        var query = mapper.toListBinsQuery(rackId, tenantId);

        // Execute query
        LocationHierarchyQueryResult result = listBinsQueryHandler.handle(query);

        // Map result to DTO
        LocationHierarchyQueryResultDTO resultDTO = mapper.toLocationHierarchyQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }
}
