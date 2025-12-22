package com.ccbsa.wms.stock.application.command;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.wms.stock.application.dto.command.AssignLocationToStockCommandDTO;
import com.ccbsa.wms.stock.application.service.command.AssignLocationToStockCommandHandler;
import com.ccbsa.wms.stock.application.service.command.dto.AssignLocationToStockCommand;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller: StockItemCommandController
 * <p>
 * Handles stock item command operations (write operations).
 * <p>
 * Responsibilities:
 * - Assign location to stock item endpoints
 * - Map DTOs to commands
 * - Return standardized API responses
 */
@RestController
@RequestMapping("/api/v1/stock-management/stock-items")
@Tag(name = "Stock Item Commands", description = "Stock item command operations")
public class StockItemCommandController {
    private final AssignLocationToStockCommandHandler assignLocationCommandHandler;

    public StockItemCommandController(AssignLocationToStockCommandHandler assignLocationCommandHandler) {
        this.assignLocationCommandHandler = assignLocationCommandHandler;
    }

    @PostMapping("/{stockItemId}/assign-location")
    @Operation(summary = "Assign Location to Stock Item", description = "Assigns a location to a stock item")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'LOCATION_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> assignLocationToStock(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable("stockItemId") String stockItemId,
                                                                   @Valid @RequestBody AssignLocationToStockCommandDTO commandDTO) {
        // Map DTO to command
        AssignLocationToStockCommand command = AssignLocationToStockCommand.builder().tenantId(com.ccbsa.common.domain.valueobject.TenantId.of(tenantId))
                .stockItemId(com.ccbsa.wms.stock.domain.core.valueobject.StockItemId.of(stockItemId))
                .locationId(com.ccbsa.wms.location.domain.core.valueobject.LocationId.of(commandDTO.getLocationId()))
                .quantity(com.ccbsa.wms.stock.domain.core.valueobject.Quantity.of(commandDTO.getQuantity())).build();

        // Execute command
        assignLocationCommandHandler.handle(command);

        return ApiResponseBuilder.ok(null);
    }
}

