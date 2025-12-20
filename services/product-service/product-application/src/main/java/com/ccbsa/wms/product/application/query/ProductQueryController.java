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
import com.ccbsa.wms.product.application.dto.query.ListProductsQueryResultDTO;
import com.ccbsa.wms.product.application.dto.query.ProductCodeUniquenessResultDTO;
import com.ccbsa.wms.product.application.dto.query.ProductQueryResultDTO;
import com.ccbsa.wms.product.application.dto.query.ValidateProductBarcodeResultDTO;
import com.ccbsa.wms.product.application.service.query.CheckProductCodeUniquenessQueryHandler;
import com.ccbsa.wms.product.application.service.query.GetProductByCodeQueryHandler;
import com.ccbsa.wms.product.application.service.query.GetProductQueryHandler;
import com.ccbsa.wms.product.application.service.query.ListProductsQueryHandler;
import com.ccbsa.wms.product.application.service.query.ValidateProductBarcodeQueryHandler;
import com.ccbsa.wms.product.application.service.query.dto.GetProductByCodeQuery;
import com.ccbsa.wms.product.application.service.query.dto.GetProductQuery;
import com.ccbsa.wms.product.application.service.query.dto.ListProductsQuery;
import com.ccbsa.wms.product.application.service.query.dto.ListProductsQueryResult;
import com.ccbsa.wms.product.application.service.query.dto.ProductCodeUniquenessResult;
import com.ccbsa.wms.product.application.service.query.dto.ProductQueryResult;
import com.ccbsa.wms.product.application.service.query.dto.ValidateProductBarcodeResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller: ProductQueryController
 * <p>
 * Handles product query operations (read operations).
 * <p>
 * Responsibilities: - Get product by ID endpoints - Check product code uniqueness endpoints - Map queries to DTOs - Return standardized API responses
 */
@RestController
@RequestMapping("/products")
@Tag(name = "Product Queries", description = "Product query operations")
public class ProductQueryController {
    private final GetProductQueryHandler getProductQueryHandler;
    private final GetProductByCodeQueryHandler getProductByCodeQueryHandler;
    private final ListProductsQueryHandler listProductsQueryHandler;
    private final CheckProductCodeUniquenessQueryHandler checkProductCodeUniquenessQueryHandler;
    private final ValidateProductBarcodeQueryHandler validateProductBarcodeQueryHandler;
    private final ProductDTOMapper mapper;

    public ProductQueryController(GetProductQueryHandler getProductQueryHandler, GetProductByCodeQueryHandler getProductByCodeQueryHandler,
                                  ListProductsQueryHandler listProductsQueryHandler, CheckProductCodeUniquenessQueryHandler checkProductCodeUniquenessQueryHandler,
                                  ValidateProductBarcodeQueryHandler validateProductBarcodeQueryHandler, ProductDTOMapper mapper) {
        this.getProductQueryHandler = getProductQueryHandler;
        this.getProductByCodeQueryHandler = getProductByCodeQueryHandler;
        this.listProductsQueryHandler = listProductsQueryHandler;
        this.checkProductCodeUniquenessQueryHandler = checkProductCodeUniquenessQueryHandler;
        this.validateProductBarcodeQueryHandler = validateProductBarcodeQueryHandler;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List Products", description = "Retrieves a list of products with optional filtering and pagination")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'LOCATION_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'VIEWER')")
    public ResponseEntity<ApiResponse<ListProductsQueryResultDTO>> listProducts(@RequestHeader("X-Tenant-Id") String tenantId, @RequestParam(required = false) Integer page,
                                                                                @RequestParam(required = false) Integer size, @RequestParam(required = false) String category,
                                                                                @RequestParam(required = false) String brand, @RequestParam(required = false) String search) {
        // Map to query
        ListProductsQuery query = mapper.toListProductsQuery(tenantId, page, size, category, brand, search);

        // Execute query
        ListProductsQueryResult result = listProductsQueryHandler.handle(query);

        // Map result to DTO
        ListProductsQueryResultDTO resultDTO = mapper.toListProductsQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get Product", description = "Retrieves a product by ID")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'LOCATION_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'VIEWER')")
    public ResponseEntity<ApiResponse<ProductQueryResultDTO>> getProduct(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable String productId) {
        // Map to query
        GetProductQuery query = mapper.toGetProductQuery(productId, tenantId);

        // Execute query
        ProductQueryResult result = getProductQueryHandler.handle(query);

        // Map result to DTO
        ProductQueryResultDTO resultDTO = mapper.toQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }

    @GetMapping("/by-code/{productCode}")
    @Operation(summary = "Get Product by Code", description = "Retrieves a product by product code")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'LOCATION_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'VIEWER')")
    public ResponseEntity<ApiResponse<ProductQueryResultDTO>> getProductByCode(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable String productCode) {
        // Map to query
        GetProductByCodeQuery query = mapper.toGetProductByCodeQuery(productCode, tenantId);

        // Execute query
        ProductQueryResult result = getProductByCodeQueryHandler.handle(query);

        // Map result to DTO
        ProductQueryResultDTO resultDTO = mapper.toQueryResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }

    @GetMapping("/check-uniqueness")
    @Operation(summary = "Check Product Code Uniqueness", description = "Checks if a product code is unique for the tenant")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<ProductCodeUniquenessResultDTO>> checkProductCodeUniqueness(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                                  @RequestParam("productCode") String productCode) {
        // Map to query
        com.ccbsa.wms.product.application.service.query.dto.CheckProductCodeUniquenessQuery query = mapper.toCheckProductCodeUniquenessQuery(productCode, tenantId);

        // Execute query
        ProductCodeUniquenessResult result = checkProductCodeUniquenessQueryHandler.handle(query);

        // Map result to DTO
        ProductCodeUniquenessResultDTO resultDTO = mapper.toProductCodeUniquenessResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }

    @GetMapping("/validate-barcode")
    @Operation(summary = "Validate Product Barcode", description = "Validates a product barcode and returns product information if found")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'OPERATOR', 'PICKER', 'STOCK_CLERK', 'RECONCILIATION_CLERK', 'RETURNS_CLERK')")
    public ResponseEntity<ApiResponse<ValidateProductBarcodeResultDTO>> validateBarcode(@RequestHeader("X-Tenant-Id") String tenantId, @RequestParam("barcode") String barcode) {
        // Map to query
        com.ccbsa.wms.product.application.service.query.dto.ValidateProductBarcodeQuery query = mapper.toValidateProductBarcodeQuery(barcode, tenantId);

        // Execute query
        ValidateProductBarcodeResult result = validateProductBarcodeQueryHandler.handle(query);

        // Map result to DTO
        ValidateProductBarcodeResultDTO resultDTO = mapper.toValidateProductBarcodeResultDTO(result);

        return ApiResponseBuilder.ok(resultDTO);
    }
}

