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
import com.ccbsa.wms.location.application.dto.command.CancelStockMovementCommandDTO;
import com.ccbsa.wms.location.application.dto.command.CancelStockMovementResultDTO;
import com.ccbsa.wms.location.application.dto.command.CompleteStockMovementCommandDTO;
import com.ccbsa.wms.location.application.dto.command.CompleteStockMovementResultDTO;
import com.ccbsa.wms.location.application.dto.command.CreateStockMovementCommandDTO;
import com.ccbsa.wms.location.application.dto.command.CreateStockMovementResultDTO;
import com.ccbsa.wms.location.application.dto.mapper.LocationDTOMapper;
import com.ccbsa.wms.location.application.service.command.CancelStockMovementCommandHandler;
import com.ccbsa.wms.location.application.service.command.CompleteStockMovementCommandHandler;
import com.ccbsa.wms.location.application.service.command.CreateStockMovementCommandHandler;
import com.ccbsa.wms.location.application.service.command.dto.CancelStockMovementCommand;
import com.ccbsa.wms.location.application.service.command.dto.CancelStockMovementResult;
import com.ccbsa.wms.location.application.service.command.dto.CompleteStockMovementCommand;
import com.ccbsa.wms.location.application.service.command.dto.CompleteStockMovementResult;
import com.ccbsa.wms.location.application.service.command.dto.CreateStockMovementCommand;
import com.ccbsa.wms.location.application.service.command.dto.CreateStockMovementResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller: StockMovementCommandController
 * <p>
 * Handles stock movement command operations (write operations).
 * <p>
 * Responsibilities:
 * - Create stock movement endpoints
 * - Complete stock movement endpoints
 * - Cancel stock movement endpoints
 * - Validate request DTOs
 * - Map DTOs to commands
 * - Return standardized API responses
 */
@RestController
@RequestMapping("/api/v1/location-management/stock-movements")
@Tag(name = "Stock Movement Commands", description = "Stock movement command operations")
public class StockMovementCommandController {
    private final CreateStockMovementCommandHandler createCommandHandler;
    private final CompleteStockMovementCommandHandler completeCommandHandler;
    private final CancelStockMovementCommandHandler cancelCommandHandler;
    private final LocationDTOMapper mapper;

    public StockMovementCommandController(CreateStockMovementCommandHandler createCommandHandler, CompleteStockMovementCommandHandler completeCommandHandler,
                                          CancelStockMovementCommandHandler cancelCommandHandler, LocationDTOMapper mapper) {
        this.createCommandHandler = createCommandHandler;
        this.completeCommandHandler = completeCommandHandler;
        this.cancelCommandHandler = cancelCommandHandler;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Create Stock Movement", description = "Initiates a stock movement between locations")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'LOCATION_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'SERVICE')")
    public ResponseEntity<ApiResponse<CreateStockMovementResultDTO>> createStockMovement(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                         @Valid @RequestBody CreateStockMovementCommandDTO commandDTO) {
        // Map DTO to command
        CreateStockMovementCommand command = mapper.toCreateStockMovementCommand(commandDTO, tenantId);

        // Execute command
        CreateStockMovementResult result = createCommandHandler.handle(command);

        // Map result to DTO
        CreateStockMovementResultDTO resultDTO = mapper.toCreateStockMovementResultDTO(result);

        return ApiResponseBuilder.created(resultDTO);
    }

    @PutMapping("/{movementId}/complete")
    @Operation(summary = "Complete Stock Movement", description = "Completes a stock movement")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'LOCATION_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'SERVICE')")
    public ResponseEntity<ApiResponse<CompleteStockMovementResultDTO>> completeStockMovement(@PathVariable String movementId, @RequestHeader("X-Tenant-Id") String tenantId,
                                                                                             @Valid @RequestBody(required = false) CompleteStockMovementCommandDTO commandDTO) {
        // Map DTO to command
        CompleteStockMovementCommand command = mapper.toCompleteStockMovementCommand(movementId, tenantId);

        // Execute command
        CompleteStockMovementResult result = completeCommandHandler.handle(command);

        // Map result to DTO
        CompleteStockMovementResultDTO resultDTO = mapper.toCompleteStockMovementResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }

    @PutMapping("/{movementId}/cancel")
    @Operation(summary = "Cancel Stock Movement", description = "Cancels a stock movement with a reason")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'LOCATION_MANAGER', 'STOCK_MANAGER', 'SERVICE')")
    public ResponseEntity<ApiResponse<CancelStockMovementResultDTO>> cancelStockMovement(@PathVariable String movementId, @RequestHeader("X-Tenant-Id") String tenantId,
                                                                                         @Valid @RequestBody CancelStockMovementCommandDTO commandDTO) {
        // Map DTO to command
        CancelStockMovementCommand command = mapper.toCancelStockMovementCommand(commandDTO, movementId, tenantId);

        // Execute command
        CancelStockMovementResult result = cancelCommandHandler.handle(command);

        // Map result to DTO
        CancelStockMovementResultDTO resultDTO = mapper.toCancelStockMovementResultDTO(result);
        // Set cancellation reason from command DTO since it's not in the result
        resultDTO.setCancellationReason(commandDTO.getCancellationReason());

        return ApiResponseBuilder.ok(resultDTO);
    }
}

