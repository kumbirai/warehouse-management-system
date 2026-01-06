package com.ccbsa.wms.product.application.command;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.wms.product.application.dto.command.CreateProductCommandDTO;
import com.ccbsa.wms.product.application.dto.command.CreateProductResultDTO;
import com.ccbsa.wms.product.application.dto.command.UpdateProductCommandDTO;
import com.ccbsa.wms.product.application.dto.command.UpdateProductResultDTO;
import com.ccbsa.wms.product.application.dto.command.UploadProductCsvResultDTO;
import com.ccbsa.wms.product.application.dto.mapper.ProductDTOMapper;
import com.ccbsa.wms.product.application.service.command.CreateProductCommandHandler;
import com.ccbsa.wms.product.application.service.command.UpdateProductCommandHandler;
import com.ccbsa.wms.product.application.service.command.UploadProductCsvCommandHandler;
import com.ccbsa.wms.product.application.service.command.dto.CreateProductResult;
import com.ccbsa.wms.product.application.service.command.dto.UpdateProductResult;
import com.ccbsa.wms.product.application.service.command.dto.UploadProductCsvResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller: ProductCommandController
 * <p>
 * Handles product command operations (write operations).
 * <p>
 * Responsibilities: - Create product endpoints - Update product endpoints - CSV upload endpoints - Validate request DTOs - Map DTOs to commands - Return standardized API responses
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product Commands", description = "Product command operations")
@RequiredArgsConstructor
public class ProductCommandController {
    private final CreateProductCommandHandler createCommandHandler;
    private final UpdateProductCommandHandler updateCommandHandler;
    private final UploadProductCsvCommandHandler uploadCsvCommandHandler;
    private final ProductDTOMapper mapper;

    @PostMapping
    @Operation(summary = "Create Product", description = "Creates a new product with barcode and unit of measure")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'SERVICE')")
    public ResponseEntity<ApiResponse<CreateProductResultDTO>> createProduct(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                             @Valid @RequestBody CreateProductCommandDTO commandDTO) {
        // Map DTO to command
        com.ccbsa.wms.product.application.service.command.dto.CreateProductCommand command = mapper.toCreateCommand(commandDTO, tenantId);

        // Execute command
        CreateProductResult result = createCommandHandler.handle(command);

        // Map result to DTO
        CreateProductResultDTO resultDTO = mapper.toCreateResultDTO(result);

        return ApiResponseBuilder.created(resultDTO);
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update Product", description = "Updates an existing product")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'SERVICE')")
    public ResponseEntity<ApiResponse<UpdateProductResultDTO>> updateProduct(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable String productId,
                                                                             @Valid @RequestBody UpdateProductCommandDTO commandDTO) {
        // Map DTO to command
        com.ccbsa.wms.product.application.service.command.dto.UpdateProductCommand command = mapper.toUpdateCommand(commandDTO, productId, tenantId);

        // Execute command
        UpdateProductResult result = updateCommandHandler.handle(command);

        // Map result to DTO
        UpdateProductResultDTO resultDTO = mapper.toUpdateResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }

    @PostMapping("/upload-csv")
    @Operation(summary = "Upload Product CSV", description = "Uploads product master data via CSV file")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'SERVICE')")
    public ResponseEntity<ApiResponse<UploadProductCsvResultDTO>> uploadProductCsv(@RequestHeader("X-Tenant-Id") String tenantId, @RequestParam("file") MultipartFile file) {
        // Map file to command
        com.ccbsa.wms.product.application.service.command.dto.UploadProductCsvCommand command = mapper.toUploadCsvCommand(file, tenantId);

        // Execute command
        UploadProductCsvResult result = uploadCsvCommandHandler.handle(command);

        // Map result to DTO
        UploadProductCsvResultDTO resultDTO = mapper.toUploadCsvResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }
}

