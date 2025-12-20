package com.ccbsa.wms.stock.application.command;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.wms.stock.application.dto.command.CreateConsignmentCommandDTO;
import com.ccbsa.wms.stock.application.dto.command.CreateConsignmentResultDTO;
import com.ccbsa.wms.stock.application.dto.command.UploadConsignmentCsvResultDTO;
import com.ccbsa.wms.stock.application.dto.command.ValidateConsignmentCommandDTO;
import com.ccbsa.wms.stock.application.dto.command.ValidateConsignmentResultDTO;
import com.ccbsa.wms.stock.application.dto.mapper.StockConsignmentDTOMapper;
import com.ccbsa.wms.stock.application.service.command.CreateConsignmentCommandHandler;
import com.ccbsa.wms.stock.application.service.command.UploadConsignmentCsvCommandHandler;
import com.ccbsa.wms.stock.application.service.command.ValidateConsignmentCommandHandler;
import com.ccbsa.wms.stock.application.service.command.dto.CreateConsignmentCommand;
import com.ccbsa.wms.stock.application.service.command.dto.CreateConsignmentResult;
import com.ccbsa.wms.stock.application.service.command.dto.UploadConsignmentCsvCommand;
import com.ccbsa.wms.stock.application.service.command.dto.UploadConsignmentCsvResult;
import com.ccbsa.wms.stock.application.service.command.dto.ValidateConsignmentCommand;
import com.ccbsa.wms.stock.application.service.command.dto.ValidateConsignmentResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller: StockConsignmentCommandController
 * <p>
 * Handles stock consignment command operations (write operations).
 * <p>
 * Responsibilities: - Create consignment endpoints - Upload CSV endpoints - Validate consignment endpoints - Map DTOs to commands - Return standardized API responses
 */
@RestController
@RequestMapping("/api/v1/stock-management/consignments")
@Tag(name = "Stock Consignment Commands", description = "Stock consignment command operations")
public class StockConsignmentCommandController {
    private final CreateConsignmentCommandHandler createCommandHandler;
    private final UploadConsignmentCsvCommandHandler uploadCsvCommandHandler;
    private final ValidateConsignmentCommandHandler validateCommandHandler;
    private final StockConsignmentDTOMapper mapper;

    public StockConsignmentCommandController(CreateConsignmentCommandHandler createCommandHandler, UploadConsignmentCsvCommandHandler uploadCsvCommandHandler,
                                             ValidateConsignmentCommandHandler validateCommandHandler, StockConsignmentDTOMapper mapper) {
        this.createCommandHandler = createCommandHandler;
        this.uploadCsvCommandHandler = uploadCsvCommandHandler;
        this.validateCommandHandler = validateCommandHandler;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Create Consignment", description = "Creates a new stock consignment")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK')")
    public ResponseEntity<ApiResponse<CreateConsignmentResultDTO>> createConsignment(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                     @Valid @RequestBody CreateConsignmentCommandDTO commandDTO) {
        // Map DTO to command
        CreateConsignmentCommand command = mapper.toCreateCommand(commandDTO, tenantId);

        // Execute command
        CreateConsignmentResult result = createCommandHandler.handle(command);

        // Map result to DTO
        CreateConsignmentResultDTO resultDTO = mapper.toCreateResultDTO(result);

        return ApiResponseBuilder.created(resultDTO);
    }

    @PostMapping("/upload-csv")
    @Operation(summary = "Upload Consignment CSV", description = "Uploads consignment data via CSV file")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK')")
    public ResponseEntity<ApiResponse<UploadConsignmentCsvResultDTO>> uploadConsignmentCsv(@RequestHeader("X-Tenant-Id") String tenantId, @RequestParam("file") MultipartFile file,
                                                                                           @RequestParam(value = "receivedBy", required = false) String receivedBy) {
        try {
            // Map file to command
            UploadConsignmentCsvCommand command = mapper.toUploadCsvCommand(file, tenantId, receivedBy);

            // Execute command
            UploadConsignmentCsvResult result = uploadCsvCommandHandler.handle(command);

            // Map result to DTO
            UploadConsignmentCsvResultDTO resultDTO = mapper.toUploadCsvResultDTO(result);

            return ApiResponseBuilder.ok(resultDTO);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Failed to read CSV file: %s", e.getMessage()), e);
        }
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate Consignment", description = "Validates consignment data before creation")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK')")
    public ResponseEntity<ApiResponse<ValidateConsignmentResultDTO>> validateConsignment(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                         @Valid @RequestBody ValidateConsignmentCommandDTO commandDTO) {
        // Map DTO to command
        ValidateConsignmentCommand command = mapper.toValidateCommand(commandDTO, tenantId);

        // Execute command
        ValidateConsignmentResult result = validateCommandHandler.handle(command);

        // Map result to DTO
        ValidateConsignmentResultDTO resultDTO = mapper.toValidateResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }
}

