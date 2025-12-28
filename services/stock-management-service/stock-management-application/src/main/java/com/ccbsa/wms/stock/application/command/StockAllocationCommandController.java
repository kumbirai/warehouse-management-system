package com.ccbsa.wms.stock.application.command;

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
import com.ccbsa.wms.stock.application.dto.command.AllocateStockCommandDTO;
import com.ccbsa.wms.stock.application.dto.command.AllocateStockResultDTO;
import com.ccbsa.wms.stock.application.dto.command.ReleaseStockAllocationCommandDTO;
import com.ccbsa.wms.stock.application.dto.command.ReleaseStockAllocationResultDTO;
import com.ccbsa.wms.stock.application.dto.mapper.StockManagementDTOMapper;
import com.ccbsa.wms.stock.application.service.command.AllocateStockCommandHandler;
import com.ccbsa.wms.stock.application.service.command.ReleaseStockAllocationCommandHandler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller: StockAllocationCommandController
 * <p>
 * Handles stock allocation command operations (write operations).
 * <p>
 * Responsibilities:
 * - Allocate stock endpoints
 * - Release stock allocation endpoints
 * - Validate request DTOs
 * - Map DTOs to commands
 * - Return standardized API responses
 */
@RestController
@RequestMapping("/api/v1/stock-management/allocations")
@Tag(name = "Stock Allocation Commands", description = "Stock allocation command operations")
public class StockAllocationCommandController {
    private final AllocateStockCommandHandler allocateCommandHandler;
    private final ReleaseStockAllocationCommandHandler releaseCommandHandler;
    private final StockManagementDTOMapper mapper;

    public StockAllocationCommandController(AllocateStockCommandHandler allocateCommandHandler, ReleaseStockAllocationCommandHandler releaseCommandHandler,
                                            StockManagementDTOMapper mapper) {
        this.allocateCommandHandler = allocateCommandHandler;
        this.releaseCommandHandler = releaseCommandHandler;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Allocate Stock", description = "Allocates stock for picking orders or reservations using FEFO")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK')")
    public ResponseEntity<ApiResponse<AllocateStockResultDTO>> allocateStock(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                             @Valid @RequestBody AllocateStockCommandDTO commandDTO) {
        // Map DTO to command
        com.ccbsa.wms.stock.application.service.command.dto.AllocateStockCommand command = mapper.toAllocateStockCommand(commandDTO, tenantId);

        // Execute command
        com.ccbsa.wms.stock.application.service.command.dto.AllocateStockResult result = allocateCommandHandler.handle(command);

        // Map result to DTO
        AllocateStockResultDTO resultDTO = mapper.toAllocateStockResultDTO(result);

        return ApiResponseBuilder.created(resultDTO);
    }

    @PutMapping("/{allocationId}/release")
    @Operation(summary = "Release Stock Allocation", description = "Releases a previously allocated stock")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<ReleaseStockAllocationResultDTO>> releaseStockAllocation(@PathVariable String allocationId, @RequestHeader("X-Tenant-Id") String tenantId,
                                                                                               @Valid @RequestBody(required = false) ReleaseStockAllocationCommandDTO commandDTO) {
        // Map DTO to command
        com.ccbsa.wms.stock.application.service.command.dto.ReleaseStockAllocationCommand command = mapper.toReleaseStockAllocationCommand(allocationId, tenantId);

        // Execute command
        com.ccbsa.wms.stock.application.service.command.dto.ReleaseStockAllocationResult result = releaseCommandHandler.handle(command);

        // Map result to DTO
        ReleaseStockAllocationResultDTO resultDTO = mapper.toReleaseStockAllocationResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }
}

