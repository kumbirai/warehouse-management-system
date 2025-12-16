package com.ccbsa.wms.product.application.query;

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
import com.ccbsa.wms.product.application.dto.mapper.ProductDTOMapper;
import com.ccbsa.wms.product.application.dto.query.ProductCodeUniquenessResultDTO;
import com.ccbsa.wms.product.application.dto.query.ProductQueryResultDTO;
import com.ccbsa.wms.product.application.service.query.CheckProductCodeUniquenessQueryHandler;
import com.ccbsa.wms.product.application.service.query.GetProductQueryHandler;
import com.ccbsa.wms.product.application.service.query.dto.GetProductQuery;
import com.ccbsa.wms.product.application.service.query.dto.ProductCodeUniquenessResult;
import com.ccbsa.wms.product.application.service.query.dto.ProductQueryResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller: ProductQueryController
 * <p>
 * Handles product query operations (read operations).
 * <p>
 * Responsibilities:
 * - Get product by ID endpoints
 * - Check product code uniqueness endpoints
 * - Map queries to DTOs
 * - Return standardized API responses
 */
@RestController
@RequestMapping("/api/v1/product-service/products")
@Tag(name = "Product Queries", description = "Product query operations")
public class ProductQueryController {
    private final GetProductQueryHandler getProductQueryHandler;
    private final CheckProductCodeUniquenessQueryHandler checkProductCodeUniquenessQueryHandler;
    private final ProductDTOMapper mapper;

    public ProductQueryController(
            GetProductQueryHandler getProductQueryHandler,
            CheckProductCodeUniquenessQueryHandler checkProductCodeUniquenessQueryHandler,
            ProductDTOMapper mapper) {
        this.getProductQueryHandler = getProductQueryHandler;
        this.checkProductCodeUniquenessQueryHandler = checkProductCodeUniquenessQueryHandler;
        this.mapper = mapper;
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get Product", description = "Retrieves a product by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<ProductQueryResultDTO>> getProduct(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String productId) {
        // Map to query
        GetProductQuery query = mapper.toGetProductQuery(productId, tenantId);

        // Execute query
        ProductQueryResult result = getProductQueryHandler.handle(query);

        // Map result to DTO
        ProductQueryResultDTO resultDTO = mapper.toQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }

    @GetMapping("/check-uniqueness")
    @Operation(summary = "Check Product Code Uniqueness", description = "Checks if a product code is unique for the tenant")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProductCodeUniquenessResultDTO>> checkProductCodeUniqueness(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam("productCode") String productCode) {
        // Map to query
        com.ccbsa.wms.product.application.service.query.dto.CheckProductCodeUniquenessQuery query =
                mapper.toCheckProductCodeUniquenessQuery(productCode, tenantId);

        // Execute query
        ProductCodeUniquenessResult result = checkProductCodeUniquenessQueryHandler.handle(query);

        // Map result to DTO
        ProductCodeUniquenessResultDTO resultDTO = mapper.toProductCodeUniquenessResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }
}

