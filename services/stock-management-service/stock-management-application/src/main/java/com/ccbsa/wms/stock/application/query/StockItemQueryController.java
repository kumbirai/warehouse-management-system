package com.ccbsa.wms.stock.application.query;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.dto.query.StockItemQueryDTO;
import com.ccbsa.wms.stock.application.dto.query.StockItemsByClassificationResponseDTO;
import com.ccbsa.wms.stock.application.service.query.GetStockItemQueryHandler;
import com.ccbsa.wms.stock.application.service.query.GetStockItemsByClassificationQueryHandler;
import com.ccbsa.wms.stock.application.service.query.GetStockItemsByProductAndLocationQueryHandler;
import com.ccbsa.wms.stock.application.service.query.GetStockItemsByProductQueryHandler;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemsByClassificationQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemsByProductAndLocationQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemsByProductQuery;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller: StockItemQueryController
 * <p>
 * Handles stock item query operations (read operations).
 * <p>
 * Responsibilities:
 * - Get stock item by ID endpoints
 * - Get stock items by classification endpoints
 * - Map query results to DTOs
 * - Return standardized API responses
 */
@RestController
@RequestMapping("/api/v1/stock-management/stock-items")
@Tag(name = "Stock Item Queries", description = "Stock item query operations")
public class StockItemQueryController {
    private final GetStockItemQueryHandler getStockItemQueryHandler;
    private final GetStockItemsByClassificationQueryHandler getStockItemsByClassificationQueryHandler;
    private final GetStockItemsByProductAndLocationQueryHandler getStockItemsByProductAndLocationQueryHandler;
    private final GetStockItemsByProductQueryHandler getStockItemsByProductQueryHandler;

    public StockItemQueryController(GetStockItemQueryHandler getStockItemQueryHandler, GetStockItemsByClassificationQueryHandler getStockItemsByClassificationQueryHandler,
                                    GetStockItemsByProductAndLocationQueryHandler getStockItemsByProductAndLocationQueryHandler,
                                    GetStockItemsByProductQueryHandler getStockItemsByProductQueryHandler) {
        this.getStockItemQueryHandler = getStockItemQueryHandler;
        this.getStockItemsByClassificationQueryHandler = getStockItemsByClassificationQueryHandler;
        this.getStockItemsByProductAndLocationQueryHandler = getStockItemsByProductAndLocationQueryHandler;
        this.getStockItemsByProductQueryHandler = getStockItemsByProductQueryHandler;
    }

    @GetMapping("/{stockItemId}")
    @Operation(summary = "Get Stock Item", description = "Gets a stock item by ID")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'LOCATION_MANAGER')")
    public ResponseEntity<ApiResponse<StockItemQueryDTO>> getStockItem(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable("stockItemId") String stockItemId) {
        // Map to query
        GetStockItemQuery query = GetStockItemQuery.builder().tenantId(TenantId.of(tenantId)).stockItemId(StockItemId.of(stockItemId)).build();

        // Execute query
        GetStockItemQueryResult result = getStockItemQueryHandler.handle(query);

        // Map result to DTO
        StockItemQueryDTO dto = mapToDTO(result);

        return ApiResponseBuilder.ok(dto);
    }

    private StockItemQueryDTO mapToDTO(GetStockItemQueryResult result) {
        StockItemQueryDTO dto = new StockItemQueryDTO();
        dto.setStockItemId(result.getStockItemId().getValueAsString());
        dto.setProductId(result.getProductId().getValueAsString());
        if (result.getLocationId() != null) {
            dto.setLocationId(result.getLocationId().getValueAsString());
        }
        dto.setQuantity(result.getQuantity().getValue());
        if (result.getExpirationDate() != null) {
            dto.setExpirationDate(result.getExpirationDate().getValue());
        }
        dto.setClassification(result.getClassification().name());
        dto.setCreatedAt(result.getCreatedAt());
        dto.setLastModifiedAt(result.getLastModifiedAt());
        return dto;
    }

    @GetMapping("/by-classification")
    @Operation(summary = "Get Stock Items by Classification", description = "Gets stock items filtered by classification")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'LOCATION_MANAGER')")
    public ResponseEntity<ApiResponse<StockItemsByClassificationResponseDTO>> getStockItemsByClassification(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                                            @RequestParam("classification") String classification) {
        // Log incoming request for debugging
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StockItemQueryController.class);
        log.info("Received request to get stock items by classification: tenantId={}, classification={}", tenantId, classification);

        // Map to query
        StockClassification stockClassification = StockClassification.valueOf(classification.toUpperCase(Locale.ROOT));
        GetStockItemsByClassificationQuery query = GetStockItemsByClassificationQuery.builder().tenantId(TenantId.of(tenantId)).classification(stockClassification).build();

        // Execute query
        List<GetStockItemQueryResult> results = getStockItemsByClassificationQueryHandler.handle(query);
        log.info("Query returned {} stock items for classification: {}, tenantId: {}", results.size(), classification, tenantId);

        // Map results to DTOs
        @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE", justification = "dtos is used in builder - SpotBugs false positive") List<StockItemQueryDTO> dtos =
                results.stream().map(this::mapToDTO).collect(Collectors.toList());

        // Wrap in response DTO to match frontend expectation: { stockItems: StockItem[] }
        StockItemsByClassificationResponseDTO response = StockItemsByClassificationResponseDTO.builder().stockItems(dtos).build();

        return ApiResponseBuilder.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get Stock Items by Product and Location", description = "Gets stock items filtered by product ID and location ID")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'LOCATION_MANAGER')")
    public ResponseEntity<ApiResponse<List<StockItemQueryDTO>>> getStockItemsByProductAndLocation(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                                  @RequestParam("productId") String productId,
                                                                                                  @RequestParam("locationId") String locationId) {
        try {
            // Map to query
            GetStockItemsByProductAndLocationQuery query =
                    GetStockItemsByProductAndLocationQuery.builder().tenantId(TenantId.of(tenantId)).productId(ProductId.of(java.util.UUID.fromString(productId)))
                            .locationId(LocationId.of(java.util.UUID.fromString(locationId))).build();

            // Execute query
            List<GetStockItemQueryResult> results = getStockItemsByProductAndLocationQueryHandler.handle(query);

            // Map results to DTOs
            List<StockItemQueryDTO> dtos = results.stream().map(this::mapToDTO).collect(Collectors.toList());

            return ApiResponseBuilder.ok(dtos);
        } catch (IllegalArgumentException e) {
            // Handle validation errors (e.g., invalid UUID format)
            return ApiResponseBuilder.error(org.springframework.http.HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request parameters: " + e.getMessage());
        } catch (Exception e) {
            // Handle unexpected errors
            return ApiResponseBuilder.error(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                    "An unexpected error occurred while retrieving stock items");
        }
    }

    @GetMapping("/by-product")
    @Operation(summary = "Get Stock Items by Product", description = "Gets stock items filtered by product ID (including items without location assignment)")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'LOCATION_MANAGER')")
    public ResponseEntity<ApiResponse<List<StockItemQueryDTO>>> getStockItemsByProduct(@RequestHeader("X-Tenant-Id") String tenantId, @RequestParam("productId") String productId) {
        try {
            // Map to query
            GetStockItemsByProductQuery query =
                    GetStockItemsByProductQuery.builder().tenantId(TenantId.of(tenantId)).productId(ProductId.of(java.util.UUID.fromString(productId))).build();

            // Execute query
            List<GetStockItemQueryResult> results = getStockItemsByProductQueryHandler.handle(query);

            // Map results to DTOs
            List<StockItemQueryDTO> dtos = results.stream().map(this::mapToDTO).collect(Collectors.toList());

            return ApiResponseBuilder.ok(dtos);
        } catch (IllegalArgumentException e) {
            return ApiResponseBuilder.error(org.springframework.http.HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request parameters: " + e.getMessage());
        } catch (Exception e) {
            return ApiResponseBuilder.error(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                    "An unexpected error occurred while retrieving stock items");
        }
    }
}

