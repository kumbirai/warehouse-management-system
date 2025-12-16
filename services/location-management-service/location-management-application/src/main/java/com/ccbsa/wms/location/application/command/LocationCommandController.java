package com.ccbsa.wms.location.application.command;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.wms.location.application.dto.command.CreateLocationCommandDTO;
import com.ccbsa.wms.location.application.dto.command.CreateLocationResultDTO;
import com.ccbsa.wms.location.application.dto.mapper.LocationDTOMapper;
import com.ccbsa.wms.location.application.service.command.CreateLocationCommandHandler;
import com.ccbsa.wms.location.application.service.command.dto.CreateLocationResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller: LocationCommandController
 * <p>
 * Handles location command operations (write operations).
 * <p>
 * Responsibilities:
 * - Create location endpoints
 * - Validate request DTOs
 * - Map DTOs to commands
 * - Return standardized API responses
 */
@RestController
@RequestMapping("/api/v1/location-management/locations")
@Tag(name = "Location Commands", description = "Location command operations")
public class LocationCommandController {
    private final CreateLocationCommandHandler commandHandler;
    private final LocationDTOMapper mapper;

    public LocationCommandController(
            CreateLocationCommandHandler commandHandler,
            LocationDTOMapper mapper) {
        this.commandHandler = commandHandler;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Create Location", description = "Creates a new warehouse location with barcode")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CreateLocationResultDTO>> createLocation(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody CreateLocationCommandDTO commandDTO) {
        // Map DTO to command
        com.ccbsa.wms.location.application.service.command.dto.CreateLocationCommand command =
                mapper.toCreateCommand(commandDTO, tenantId);

        // Execute command
        CreateLocationResult result = commandHandler.handle(command);

        // Map result to DTO
        CreateLocationResultDTO resultDTO = mapper.toCreateResultDTO(result);

        return ApiResponseBuilder.created(resultDTO);
    }
}

