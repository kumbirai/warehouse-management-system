package com.ccbsa.wms.location.application.command;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.wms.location.application.dto.command.CreateLocationCommandDTO;
import com.ccbsa.wms.location.application.dto.command.CreateLocationResultDTO;
import com.ccbsa.wms.location.application.dto.command.UpdateLocationCommandDTO;
import com.ccbsa.wms.location.application.dto.command.UpdateLocationStatusCommandDTO;
import com.ccbsa.wms.location.application.dto.mapper.LocationDTOMapper;
import com.ccbsa.wms.location.application.dto.query.LocationQueryResultDTO;
import com.ccbsa.wms.location.application.service.command.CreateLocationCommandHandler;
import com.ccbsa.wms.location.application.service.command.UpdateLocationCommandHandler;
import com.ccbsa.wms.location.application.service.command.UpdateLocationStatusCommandHandler;
import com.ccbsa.wms.location.application.service.command.dto.CreateLocationCommand;
import com.ccbsa.wms.location.application.service.command.dto.CreateLocationResult;
import com.ccbsa.wms.location.application.service.command.dto.UpdateLocationCommand;
import com.ccbsa.wms.location.application.service.command.dto.UpdateLocationStatusCommand;
import com.ccbsa.wms.location.application.service.query.GetLocationQueryHandler;
import com.ccbsa.wms.location.application.service.query.dto.GetLocationQuery;
import com.ccbsa.wms.location.application.service.query.dto.LocationQueryResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller: LocationCommandController
 * <p>
 * Handles location command operations (write operations).
 * <p>
 * Responsibilities: - Create location endpoints - Validate request DTOs - Map DTOs to commands - Return standardized API responses
 */
@RestController
@RequestMapping("/locations")
@Tag(name = "Location Commands", description = "Location command operations")
public class LocationCommandController {
    private final CreateLocationCommandHandler createCommandHandler;
    private final UpdateLocationCommandHandler updateCommandHandler;
    private final UpdateLocationStatusCommandHandler updateStatusCommandHandler;
    private final GetLocationQueryHandler getLocationQueryHandler;
    private final LocationDTOMapper mapper;

    public LocationCommandController(CreateLocationCommandHandler createCommandHandler, UpdateLocationCommandHandler updateCommandHandler,
                                     UpdateLocationStatusCommandHandler updateStatusCommandHandler, GetLocationQueryHandler getLocationQueryHandler, LocationDTOMapper mapper) {
        this.createCommandHandler = createCommandHandler;
        this.updateCommandHandler = updateCommandHandler;
        this.updateStatusCommandHandler = updateStatusCommandHandler;
        this.getLocationQueryHandler = getLocationQueryHandler;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Create Location", description = "Creates a new warehouse location with barcode")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'LOCATION_MANAGER')")
    public ResponseEntity<ApiResponse<CreateLocationResultDTO>> createLocation(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                               @Valid @RequestBody CreateLocationCommandDTO commandDTO) {
        // Map DTO to command
        CreateLocationCommand command = mapper.toCreateCommand(commandDTO, tenantId);

        // Execute command
        CreateLocationResult result = createCommandHandler.handle(command);

        // Map result to DTO
        CreateLocationResultDTO resultDTO = mapper.toCreateResultDTO(result);

        return ApiResponseBuilder.created(resultDTO);
    }

    @PutMapping("/{locationId}")
    @Operation(summary = "Update Location", description = "Updates an existing location's coordinates, barcode, and description")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'LOCATION_MANAGER')")
    public ResponseEntity<ApiResponse<LocationQueryResultDTO>> updateLocation(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable String locationId,
                                                                              @Valid @RequestBody UpdateLocationCommandDTO commandDTO) {
        // Map DTO to command
        UpdateLocationCommand command = mapper.toUpdateLocationCommand(commandDTO, locationId, tenantId);

        // Execute command to update location
        updateCommandHandler.handle(command);

        // Fetch updated location to return full details
        GetLocationQuery query = mapper.toGetLocationQuery(locationId, tenantId);
        LocationQueryResult result = getLocationQueryHandler.handle(query);

        // Map result to DTO
        LocationQueryResultDTO resultDTO = mapper.toQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }

    @PutMapping("/{locationId}/status")
    @Operation(summary = "Update Location Status", description = "Updates the status of an existing location")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'LOCATION_MANAGER')")
    public ResponseEntity<ApiResponse<LocationQueryResultDTO>> updateLocationStatus(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable String locationId,
                                                                                    @Valid @RequestBody UpdateLocationStatusCommandDTO commandDTO) {
        // Map DTO to command
        UpdateLocationStatusCommand command = mapper.toUpdateStatusCommand(commandDTO, locationId, tenantId);

        // Execute command to update status
        updateStatusCommandHandler.handle(command);

        // Fetch updated location to return full details
        GetLocationQuery query = mapper.toGetLocationQuery(locationId, tenantId);
        LocationQueryResult result = getLocationQueryHandler.handle(query);

        // Map result to DTO
        LocationQueryResultDTO resultDTO = mapper.toQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }
}

