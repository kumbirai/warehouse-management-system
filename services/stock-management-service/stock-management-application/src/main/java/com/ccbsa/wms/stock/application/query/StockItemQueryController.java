package com.ccbsa.wms.stock.application.query;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.stock.application.dto.query.StockAvailabilityFefoRequestDTO;
import com.ccbsa.wms.stock.application.dto.query.StockAvailabilityFefoResponseDTO;
import com.ccbsa.wms.stock.application.dto.query.StockAvailabilityItemDTO;
import com.ccbsa.wms.stock.application.dto.query.StockAvailabilityRequestDTO;
import com.ccbsa.wms.stock.application.dto.query.StockAvailabilityResponseDTO;
import com.ccbsa.wms.stock.application.dto.query.StockItemQueryDTO;
import com.ccbsa.wms.stock.application.dto.query.StockItemsByClassificationResponseDTO;
import com.ccbsa.wms.stock.application.service.exception.ProductServiceException;
import com.ccbsa.wms.stock.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.stock.application.service.query.GetFEFOStockItemsQueryHandler;
import com.ccbsa.wms.stock.application.service.query.GetStockItemQueryHandler;
import com.ccbsa.wms.stock.application.service.query.GetStockItemsByClassificationQueryHandler;
import com.ccbsa.wms.stock.application.service.query.GetStockItemsByProductAndLocationQueryHandler;
import com.ccbsa.wms.stock.application.service.query.GetStockItemsByProductQueryHandler;
import com.ccbsa.wms.stock.application.service.query.GetStockItemsQueryHandler;
import com.ccbsa.wms.stock.application.service.query.QueryStockAvailabilityForProductsQueryHandler;
import com.ccbsa.wms.stock.application.service.query.dto.GetFEFOStockItemsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemsByClassificationQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemsByProductAndLocationQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemsByProductQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.QueryStockAvailabilityForProductsQuery;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

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
@RequestMapping("/api/v1/stock-management")
@Tag(name = "Stock Item Queries", description = "Stock item query operations")
@Slf4j
public class StockItemQueryController {
    private final GetStockItemQueryHandler getStockItemQueryHandler;
    private final GetStockItemsByClassificationQueryHandler getStockItemsByClassificationQueryHandler;
    private final GetStockItemsByProductAndLocationQueryHandler getStockItemsByProductAndLocationQueryHandler;
    private final GetStockItemsByProductQueryHandler getStockItemsByProductQueryHandler;
    private final GetStockItemsQueryHandler getStockItemsQueryHandler;
    private final GetFEFOStockItemsQueryHandler fefoStockItemsQueryHandler;
    private final QueryStockAvailabilityForProductsQueryHandler queryStockAvailabilityForProductsQueryHandler;
    private final ProductServicePort productServicePort;

    public StockItemQueryController(GetStockItemQueryHandler getStockItemQueryHandler, GetStockItemsByClassificationQueryHandler getStockItemsByClassificationQueryHandler,
                                    GetStockItemsByProductAndLocationQueryHandler getStockItemsByProductAndLocationQueryHandler,
                                    GetStockItemsByProductQueryHandler getStockItemsByProductQueryHandler, GetStockItemsQueryHandler getStockItemsQueryHandler,
                                    GetFEFOStockItemsQueryHandler fefoStockItemsQueryHandler,
                                    QueryStockAvailabilityForProductsQueryHandler queryStockAvailabilityForProductsQueryHandler, ProductServicePort productServicePort) {
        this.getStockItemQueryHandler = getStockItemQueryHandler;
        this.getStockItemsByClassificationQueryHandler = getStockItemsByClassificationQueryHandler;
        this.getStockItemsByProductAndLocationQueryHandler = getStockItemsByProductAndLocationQueryHandler;
        this.getStockItemsByProductQueryHandler = getStockItemsByProductQueryHandler;
        this.getStockItemsQueryHandler = getStockItemsQueryHandler;
        this.fefoStockItemsQueryHandler = fefoStockItemsQueryHandler;
        this.queryStockAvailabilityForProductsQueryHandler = queryStockAvailabilityForProductsQueryHandler;
        this.productServicePort = productServicePort;
    }

    @GetMapping("/stock-items/all")
    @Operation(summary = "Get All Stock Items", description = "Gets all stock items for a tenant")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'LOCATION_MANAGER', 'SERVICE')")
    public ResponseEntity<ApiResponse<StockItemsByClassificationResponseDTO>> getAllStockItems(@RequestHeader("X-Tenant-Id") String tenantId) {
        // Log incoming request for debugging
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StockItemQueryController.class);
        log.info("Received request to get all stock items: tenantId={}", tenantId);

        // Map to query
        GetStockItemsQuery query = GetStockItemsQuery.builder().tenantId(TenantId.of(tenantId)).build();

        // Execute query
        List<GetStockItemQueryResult> results = getStockItemsQueryHandler.handle(query);
        log.info("Query returned {} stock items for tenantId: {}", results.size(), tenantId);

        // Map results to DTOs
        @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE", justification = "dtos is used in builder - SpotBugs false positive") List<StockItemQueryDTO> dtos =
                results.stream().map(this::mapToDTO).collect(Collectors.toList());

        // Wrap in response DTO to match frontend expectation: { stockItems: StockItem[] }
        StockItemsByClassificationResponseDTO response = StockItemsByClassificationResponseDTO.builder().stockItems(dtos).build();

        return ApiResponseBuilder.ok(response);
    }

    private StockItemQueryDTO mapToDTO(GetStockItemQueryResult result) {
        StockItemQueryDTO dto = new StockItemQueryDTO();
        dto.setStockItemId(result.getStockItemId().getValueAsString());
        dto.setProductId(result.getProductId().getValueAsString());
        dto.setProductCode(result.getProductCode());
        dto.setProductDescription(result.getProductDescription());
        if (result.getLocationId() != null) {
            dto.setLocationId(result.getLocationId().getValueAsString());
        }
        dto.setLocationCode(result.getLocationCode());
        dto.setLocationName(result.getLocationName());
        dto.setLocationHierarchy(result.getLocationHierarchy());
        dto.setQuantity(result.getQuantity().getValue());
        dto.setAllocatedQuantity(result.getAllocatedQuantity() != null ? result.getAllocatedQuantity().getValue() : 0);
        if (result.getExpirationDate() != null) {
            dto.setExpirationDate(result.getExpirationDate().getValue());
        }
        dto.setClassification(result.getClassification().name());
        dto.setCreatedAt(result.getCreatedAt());
        dto.setLastModifiedAt(result.getLastModifiedAt());
        return dto;
    }

    @GetMapping("/stock-items/{stockItemId}")
    @Operation(summary = "Get Stock Item", description = "Gets a stock item by ID")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'LOCATION_MANAGER', 'SERVICE')")
    public ResponseEntity<ApiResponse<StockItemQueryDTO>> getStockItem(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable("stockItemId") String stockItemId) {
        // Map to query
        GetStockItemQuery query = GetStockItemQuery.builder().tenantId(TenantId.of(tenantId)).stockItemId(StockItemId.of(stockItemId)).build();

        // Execute query
        GetStockItemQueryResult result = getStockItemQueryHandler.handle(query);

        // Map result to DTO
        StockItemQueryDTO dto = mapToDTO(result);

        return ApiResponseBuilder.ok(dto);
    }

    @GetMapping("/stock-items/by-classification")
    @Operation(summary = "Get Stock Items by Classification", description = "Gets stock items filtered by classification")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'LOCATION_MANAGER', 'SERVICE')")
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

    @GetMapping("/stock-items")
    @Operation(summary = "Get Stock Items by Product and Location", description = "Gets stock items filtered by product ID and optionally by location ID")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'LOCATION_MANAGER', 'SERVICE')")
    public ResponseEntity<ApiResponse<StockItemsByClassificationResponseDTO>> getStockItemsByProductAndLocation(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                                                @RequestParam("productId") String productId,
                                                                                                                @RequestParam(value = "locationId", required = false)
                                                                                                                String locationId) {
        try {
            List<GetStockItemQueryResult> results;

            // If locationId is not provided, use the by-product endpoint logic
            if (locationId == null || locationId.isEmpty()) {
                GetStockItemsByProductQuery query =
                        GetStockItemsByProductQuery.builder().tenantId(TenantId.of(tenantId)).productId(ProductId.of(java.util.UUID.fromString(productId))).build();

                results = getStockItemsByProductQueryHandler.handle(query);
            } else {
                // Map to query with locationId
                GetStockItemsByProductAndLocationQuery query =
                        GetStockItemsByProductAndLocationQuery.builder().tenantId(TenantId.of(tenantId)).productId(ProductId.of(java.util.UUID.fromString(productId)))
                                .locationId(LocationId.of(java.util.UUID.fromString(locationId))).build();

                // Execute query
                results = getStockItemsByProductAndLocationQueryHandler.handle(query);
            }

            // Map results to DTOs
            @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE", justification = "dtos is used in builder - SpotBugs false positive") List<StockItemQueryDTO> dtos =
                    results.stream().map(this::mapToDTO).collect(Collectors.toList());

            // Wrap in response DTO to match test expectation: { stockItems: StockItem[] }
            StockItemsByClassificationResponseDTO response = StockItemsByClassificationResponseDTO.builder().stockItems(dtos).build();

            return ApiResponseBuilder.ok(response);
        } catch (IllegalArgumentException e) {
            // Handle validation errors (e.g., invalid UUID format)
            return ApiResponseBuilder.error(org.springframework.http.HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request parameters: " + e.getMessage());
        } catch (Exception e) {
            // Handle unexpected errors
            return ApiResponseBuilder.error(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                    "An unexpected error occurred while retrieving stock items");
        }
    }

    @GetMapping("/stock-items/by-product")
    @Operation(summary = "Get Stock Items by Product", description = "Gets stock items filtered by product ID (including items without location assignment)")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'LOCATION_MANAGER', 'SERVICE')")
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

    @PostMapping("/stock/query-availability-fefo")
    @Operation(summary = "Query Stock Availability by FEFO", description = "Queries available stock for a product sorted by FEFO (First Expiring First Out)")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'LOCATION_MANAGER', 'SERVICE')")
    public ResponseEntity<ApiResponse<StockAvailabilityFefoResponseDTO>> queryStockAvailabilityFefo(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                                    @RequestBody StockAvailabilityFefoRequestDTO request) {
        try {
            log.debug("Received FEFO stock availability query: productCode={}, quantity={}, tenantId={}", request.getProductCode(), request.getQuantity(), tenantId);

            // Validate request
            if (request.getProductCode() == null || request.getProductCode().trim().isEmpty()) {
                return ApiResponseBuilder.error(org.springframework.http.HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Product code is required");
            }
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                return ApiResponseBuilder.error(org.springframework.http.HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Quantity must be greater than 0");
            }

            // Get ProductId from productCode
            TenantId tenantIdValue = TenantId.of(tenantId);
            ProductCode productCode = ProductCode.of(request.getProductCode());
            ProductServicePort.ProductInfo productInfo = productServicePort.getProductByCode(productCode, tenantIdValue)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found for code: " + request.getProductCode()));

            ProductId productId = ProductId.of(productInfo.getProductId());

            // Query stock items using FEFO handler
            GetFEFOStockItemsQuery fefoQuery = GetFEFOStockItemsQuery.builder().tenantId(tenantIdValue).productId(productId).locationId(null) // All locations for FEFO
                    .build();

            List<GetStockItemQueryResult> stockItems = fefoStockItemsQueryHandler.handle(fefoQuery);

            // Limit to required quantity (take first N items that satisfy the quantity)
            List<GetStockItemQueryResult> limitedItems = limitToQuantity(stockItems, request.getQuantity());

            // Map to response DTO
            List<StockAvailabilityItemDTO> stockItemDTOs = limitedItems.stream()
                    .map(item -> StockAvailabilityItemDTO.builder().locationId(item.getLocationId() != null ? item.getLocationId().getValueAsString() : null)
                            .availableQuantity(item.getQuantity().getValue() - (item.getAllocatedQuantity() != null ? item.getAllocatedQuantity().getValue() : 0))
                            .expirationDate(item.getExpirationDate() != null ? item.getExpirationDate().getValue() : null).stockItemId(item.getStockItemId().getValueAsString())
                            .build()).collect(Collectors.toList());

            StockAvailabilityFefoResponseDTO response = StockAvailabilityFefoResponseDTO.builder().productCode(request.getProductCode()).stockItems(stockItemDTOs).build();

            return ApiResponseBuilder.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for FEFO stock availability query: {}", e.getMessage());
            return ApiResponseBuilder.error(org.springframework.http.HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", e.getMessage());
        } catch (ProductServiceException e) {
            log.error("Product service unavailable while querying stock availability by FEFO: {}", e.getMessage(), e);
            return ApiResponseBuilder.error(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                    "Product service is temporarily unavailable. Please try again later.");
        } catch (Exception e) {
            log.error("Error querying stock availability by FEFO", e);
            return ApiResponseBuilder.error(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                    "An unexpected error occurred while querying stock availability");
        }
    }

    /**
     * Limits stock items to satisfy the required quantity.
     * Takes items in FEFO order until quantity requirement is met.
     */
    private List<GetStockItemQueryResult> limitToQuantity(List<GetStockItemQueryResult> stockItems, Integer requiredQuantity) {
        int remainingQuantity = requiredQuantity;
        List<GetStockItemQueryResult> selectedItems = new java.util.ArrayList<>();

        for (GetStockItemQueryResult item : stockItems) {
            if (remainingQuantity <= 0) {
                break;
            }

            int availableQuantity = item.getQuantity().getValue() - (item.getAllocatedQuantity() != null ? item.getAllocatedQuantity().getValue() : 0);

            if (availableQuantity > 0) {
                selectedItems.add(item);
                remainingQuantity -= availableQuantity;
            }
        }

        return selectedItems;
    }

    @PostMapping("/stock/query-availability")
    @Operation(summary = "Query Stock Availability for Multiple Products", description = "Queries available stock for multiple products sorted by FEFO")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'STOCK_MANAGER', 'OPERATOR', 'STOCK_CLERK', 'LOCATION_MANAGER', 'SERVICE')")
    public ResponseEntity<ApiResponse<StockAvailabilityResponseDTO>> queryStockAvailability(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                            @RequestBody StockAvailabilityRequestDTO request) {
        try {
            log.debug("Received stock availability query for {} products, tenantId={}", request.getProductQuantities() != null ? request.getProductQuantities().size() : 0,
                    tenantId);

            // Validate request
            if (request.getProductQuantities() == null || request.getProductQuantities().isEmpty()) {
                return ApiResponseBuilder.error(org.springframework.http.HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Product quantities map is required and cannot be empty");
            }

            // Convert product codes to ProductIds
            TenantId tenantIdValue = TenantId.of(tenantId);
            Map<ProductId, Integer> productQuantities = new HashMap<>();

            for (Map.Entry<String, Integer> entry : request.getProductQuantities().entrySet()) {
                String productCode = entry.getKey();
                Integer quantity = entry.getValue();

                if (productCode == null || productCode.trim().isEmpty()) {
                    log.warn("Skipping invalid product code: {}", productCode);
                    continue;
                }
                if (quantity == null || quantity <= 0) {
                    log.warn("Skipping invalid quantity for product code: {}, quantity: {}", productCode, quantity);
                    continue;
                }

                ProductCode productCodeValue = ProductCode.of(productCode);
                ProductServicePort.ProductInfo productInfo = productServicePort.getProductByCode(productCodeValue, tenantIdValue).orElse(null);

                if (productInfo == null) {
                    log.warn("Product not found for code: {}", productCode);
                    continue;
                }

                ProductId productId = ProductId.of(productInfo.getProductId());
                productQuantities.put(productId, quantity);
            }

            if (productQuantities.isEmpty()) {
                return ApiResponseBuilder.error(org.springframework.http.HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "No valid products found");
            }

            // Query stock availability for all products
            QueryStockAvailabilityForProductsQuery query = QueryStockAvailabilityForProductsQuery.builder().tenantId(tenantIdValue).productQuantities(productQuantities).build();

            Map<ProductId, List<GetStockItemQueryResult>> results = queryStockAvailabilityForProductsQueryHandler.handle(query);

            // Map results to response DTO (grouped by product code)
            Map<String, List<StockAvailabilityItemDTO>> stockByProduct = new HashMap<>();

            for (Map.Entry<ProductId, List<GetStockItemQueryResult>> entry : results.entrySet()) {
                ProductId productId = entry.getKey();
                List<GetStockItemQueryResult> stockItems = entry.getValue();

                // Get product code for this ProductId
                ProductServicePort.ProductInfo productInfo = productServicePort.getProductById(productId, tenantIdValue).orElse(null);

                if (productInfo == null) {
                    log.warn("Product info not found for ProductId: {}", productId.getValueAsString());
                    continue;
                }

                String productCode = productInfo.getProductCode();

                List<StockAvailabilityItemDTO> stockItemDTOs = stockItems.stream()
                        .map(item -> StockAvailabilityItemDTO.builder().locationId(item.getLocationId() != null ? item.getLocationId().getValueAsString() : null)
                                .availableQuantity(item.getQuantity().getValue() - (item.getAllocatedQuantity() != null ? item.getAllocatedQuantity().getValue() : 0))
                                .expirationDate(item.getExpirationDate() != null ? item.getExpirationDate().getValue() : null).stockItemId(item.getStockItemId().getValueAsString())
                                .build()).collect(Collectors.toList());

                stockByProduct.put(productCode, stockItemDTOs);
            }

            StockAvailabilityResponseDTO response = StockAvailabilityResponseDTO.builder().stockByProduct(stockByProduct).build();

            return ApiResponseBuilder.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for stock availability query: {}", e.getMessage());
            return ApiResponseBuilder.error(org.springframework.http.HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", e.getMessage());
        } catch (ProductServiceException e) {
            log.error("Product service unavailable while querying stock availability: {}", e.getMessage(), e);
            return ApiResponseBuilder.error(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                    "Product service is temporarily unavailable. Please try again later.");
        } catch (Exception e) {
            log.error("Error querying stock availability", e);
            return ApiResponseBuilder.error(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                    "An unexpected error occurred while querying stock availability");
        }
    }
}

