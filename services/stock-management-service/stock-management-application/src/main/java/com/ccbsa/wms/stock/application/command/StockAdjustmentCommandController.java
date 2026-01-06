package com.ccbsa.wms.stock.application.command;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.wms.stock.application.dto.command.AdjustStockCommandDTO;
import com.ccbsa.wms.stock.application.dto.command.AdjustStockResultDTO;
import com.ccbsa.wms.stock.application.dto.mapper.StockManagementDTOMapper;
import com.ccbsa.wms.stock.application.service.command.AdjustStockCommandHandler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller: StockAdjustmentCommandController
 * <p>
 * Handles stock adjustment command operations (write operations).
 * <p>
 * Responsibilities:
 * - Adjust stock levels endpoints
 * - Validate request DTOs
 * - Map DTOs to commands
 * - Return standardized API responses
 */
@RestController
@RequestMapping("/api/v1/stock-management/adjustments")
@Tag(name = "Stock Adjustment Commands", description = "Stock adjustment command operations")
public class StockAdjustmentCommandController {
    private final AdjustStockCommandHandler adjustCommandHandler;
    private final StockManagementDTOMapper mapper;

    public StockAdjustmentCommandController(AdjustStockCommandHandler adjustCommandHandler, StockManagementDTOMapper mapper) {
        this.adjustCommandHandler = adjustCommandHandler;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Adjust Stock", description = "Adjusts stock levels (increase or decrease) with reason tracking")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'SERVICE')")
    public ResponseEntity<ApiResponse<AdjustStockResultDTO>> adjustStock(@RequestHeader("X-Tenant-Id") String tenantId, @Valid @RequestBody AdjustStockCommandDTO commandDTO) {
        // Map DTO to command
        com.ccbsa.wms.stock.application.service.command.dto.AdjustStockCommand command = mapper.toAdjustStockCommand(commandDTO, tenantId);

        // Execute command
        com.ccbsa.wms.stock.application.service.command.dto.AdjustStockResult result = adjustCommandHandler.handle(command);

        // Map result to DTO
        AdjustStockResultDTO resultDTO = mapper.toAdjustStockResultDTO(result);

        return ApiResponseBuilder.created(resultDTO);
    }
}

