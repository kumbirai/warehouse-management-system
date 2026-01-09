package com.ccbsa.wms.location.dataaccess.adapter;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.application.service.port.service.StockManagementServicePort;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.extern.slf4j.Slf4j;

/**
 * Adapter: StockManagementServiceAdapter
 * <p>
 * Implements StockManagementServicePort for validating stock items via Stock Management Service.
 * Calls stock-management-service REST API to validate stock items and check quantity availability.
 */
@Component
@Slf4j
public class StockManagementServiceAdapter implements StockManagementServicePort {
    private static final ParameterizedTypeReference<ApiResponse<StockItemResponse>> STOCK_ITEM_RESPONSE_TYPE = new ParameterizedTypeReference<ApiResponse<StockItemResponse>>() {
    };
    private static final ParameterizedTypeReference<ApiResponse<StockItemsByClassificationResponseDTO>> STOCK_ITEMS_LIST_RESPONSE_TYPE =
            new ParameterizedTypeReference<ApiResponse<StockItemsByClassificationResponseDTO>>() {
            };
    private final RestTemplate restTemplate;
    private final String stockManagementServiceUrl;

    public StockManagementServiceAdapter(RestTemplate restTemplate, @Value("${stock-management.service.url:http://stock-management-service}") String stockManagementServiceUrl) {
        this.restTemplate = restTemplate;
        this.stockManagementServiceUrl = stockManagementServiceUrl;
    }

    @Override
    public StockItemValidationResult validateStockItem(String stockItemId, Quantity quantity, TenantId tenantId) {
        log.debug("Validating stock item from stock-management-service: stockItemId={}, quantity={}, tenantId={}", stockItemId, quantity.getValue(), tenantId.getValue());

        try {
            String url = String.format("%s/api/v1/stock-management/stock-items/%s", stockManagementServiceUrl, stockItemId);
            log.debug("Calling stock management service: {}", url);

            // Service-to-service authentication is handled automatically by ServiceAccountAuthenticationInterceptor
            // The interceptor will:
            // 1. Forward Authorization header from HTTP request context (if available)
            // 2. Use service account token for event-driven calls (no HTTP context)
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-Id", tenantId.getValue());

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<StockItemResponse>> response = restTemplate.exchange(url, HttpMethod.GET, entity, STOCK_ITEM_RESPONSE_TYPE);

            ApiResponse<StockItemResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                StockItemResponse stockItemResponse = responseBody.getData();

                // Validate stock item exists
                if (stockItemResponse.getStockItemId() == null || stockItemResponse.getStockItemId().isEmpty()) {
                    log.warn("Stock item response missing stockItemId");
                    return StockItemValidationResult.invalid("Stock item not found");
                }

                // Validate available quantity is sufficient (total - allocated)
                int totalQuantity = stockItemResponse.getQuantity() != null ? stockItemResponse.getQuantity() : 0;
                int allocatedQuantity = stockItemResponse.getAllocatedQuantity() != null ? stockItemResponse.getAllocatedQuantity() : 0;
                int availableQuantity = Math.max(0, totalQuantity - allocatedQuantity);

                if (availableQuantity < quantity.getValue()) {
                    log.warn("Stock item has insufficient available quantity: total={}, allocated={}, available={}, required={}", totalQuantity, allocatedQuantity,
                            availableQuantity, quantity.getValue());
                    return StockItemValidationResult.invalid(String.format("Insufficient quantity. Available: %d, Required: %d", availableQuantity, quantity.getValue()));
                }

                // Extract product ID
                ProductId productId = stockItemResponse.getProductId() != null ? ProductId.of(stockItemResponse.getProductId()) : null;

                log.debug("Stock item validated successfully: stockItemId={}, productId={}, quantity={}", stockItemResponse.getStockItemId(), productId,
                        stockItemResponse.getQuantity());
                return StockItemValidationResult.valid(productId);
            }

            log.warn("Stock management service returned unexpected response: status={}", response.getStatusCode());
            return StockItemValidationResult.invalid("Stock management service returned unexpected response");
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Stock item not found: stockItemId={}", stockItemId);
            return StockItemValidationResult.invalid("Stock item not found: " + stockItemId);
        } catch (RestClientException e) {
            log.error("Failed to validate stock item from stock-management-service: stockItemId={}", stockItemId, e);
            return StockItemValidationResult.invalid("Stock management service is temporarily unavailable. Please try again later.");
        } catch (Exception e) {
            log.error("Unexpected error validating stock item: stockItemId={}", stockItemId, e);
            return StockItemValidationResult.invalid("Stock management service error: " + e.getMessage());
        }
    }

    @Override
    public StockItemQueryResult findStockItemByProductAndLocation(ProductId productId, LocationId locationId, TenantId tenantId) {
        log.debug("Finding stock item by product and location: productId={}, locationId={}, tenantId={}", productId.getValueAsString(), locationId.getValueAsString(),
                tenantId.getValue());

        try {
            // Use stock items query endpoint to find stock items by product and location
            String url = String.format("%s/api/v1/stock-management/stock-items?productId=%s&locationId=%s", stockManagementServiceUrl, productId.getValueAsString(),
                    locationId.getValueAsString());
            log.debug("Calling stock management service: {}", url);

            // Service-to-service authentication is handled automatically by ServiceAccountAuthenticationInterceptor
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-Id", tenantId.getValue());

            HttpEntity<?> entity = new HttpEntity<>(headers);

            // Query stock items by product and location
            ResponseEntity<ApiResponse<StockItemsByClassificationResponseDTO>> response = restTemplate.exchange(url, HttpMethod.GET, entity, STOCK_ITEMS_LIST_RESPONSE_TYPE);

            ApiResponse<StockItemsByClassificationResponseDTO> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                StockItemsByClassificationResponseDTO responseData = responseBody.getData();
                List<StockItemResponse> stockItems = responseData.getStockItems() != null ? responseData.getStockItems() : List.of();

                if (stockItems.isEmpty()) {
                    log.debug("No stock items found for product: {} at location: {}", productId.getValueAsString(), locationId.getValueAsString());
                    return StockItemQueryResult.notFound(
                            String.format("No stock items found for product: %s at location: %s", productId.getValueAsString(), locationId.getValueAsString()));
                }

                // Calculate total available quantity across all stock items at this location
                // This ensures we validate against total available, not just a single stock item
                int totalAvailable = stockItems.stream().mapToInt(item -> {
                    int totalQty = item.getQuantity() != null ? item.getQuantity() : 0;
                    int allocatedQty = item.getAllocatedQuantity() != null ? item.getAllocatedQuantity() : 0;
                    return Math.max(0, totalQty - allocatedQty);
                }).sum();

                log.debug("Found {} stock item(s) for product: {} at location: {} with total available quantity: {}", stockItems.size(), productId.getValueAsString(),
                        locationId.getValueAsString(), totalAvailable);

                // Return the first stock item found (for stock movements, we typically need one item)
                // In the future, we might want to implement FEFO logic here
                StockItemResponse firstItem = stockItems.get(0);
                if (firstItem.getStockItemId() == null || firstItem.getStockItemId().isEmpty()) {
                    log.warn("Stock item response missing stockItemId");
                    return StockItemQueryResult.notFound("Stock item response missing stockItemId");
                }

                log.debug("Found stock item: {} for product: {} at location: {} (total available: {})", firstItem.getStockItemId(), productId.getValueAsString(),
                        locationId.getValueAsString(), totalAvailable);
                return StockItemQueryResult.found(firstItem.getStockItemId(), totalAvailable);
            }

            log.warn("Stock management service returned unexpected response: status={}", response.getStatusCode());
            return StockItemQueryResult.notFound("Stock management service returned unexpected response");
        } catch (Exception e) {
            log.error("Failed to find stock item by product and location: productId={}, locationId={}", productId.getValueAsString(), locationId.getValueAsString(), e);
            return StockItemQueryResult.notFound("Stock management service error: " + e.getMessage());
        }
    }

    @Override
    public StockItemQueryResult findStockItemByProduct(ProductId productId, TenantId tenantId) {
        log.debug("Finding stock item by product: productId={}, tenantId={}", productId.getValueAsString(), tenantId.getValue());

        try {
            // Use stock items query endpoint to find stock items by product only
            String url = String.format("%s/api/v1/stock-management/stock-items/by-product?productId=%s", stockManagementServiceUrl, productId.getValueAsString());
            log.debug("Calling stock management service: {}", url);

            // Service-to-service authentication is handled automatically by ServiceAccountAuthenticationInterceptor
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-Id", tenantId.getValue());

            HttpEntity<?> entity = new HttpEntity<>(headers);

            // Query stock items by product only
            // Note: /by-product endpoint returns List<StockItemQueryDTO> directly, not wrapped
            ParameterizedTypeReference<ApiResponse<List<StockItemResponse>>> byProductResponseType = new ParameterizedTypeReference<ApiResponse<List<StockItemResponse>>>() {
            };
            ResponseEntity<ApiResponse<List<StockItemResponse>>> response = restTemplate.exchange(url, HttpMethod.GET, entity, byProductResponseType);

            ApiResponse<List<StockItemResponse>> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                List<StockItemResponse> stockItems = responseBody.getData();

                if (stockItems.isEmpty()) {
                    log.debug("No stock items found for product: {}", productId.getValueAsString());
                    return StockItemQueryResult.notFound(String.format("No stock items found for product: %s", productId.getValueAsString()));
                }

                // Return the first stock item found (prefer items without location, then items with location)
                StockItemResponse firstItem =
                        stockItems.stream().filter(item -> item.getLocationId() == null || item.getLocationId().isEmpty()).findFirst().orElse(stockItems.get(0));

                if (firstItem.getStockItemId() == null || firstItem.getStockItemId().isEmpty()) {
                    log.warn("Stock item response missing stockItemId");
                    return StockItemQueryResult.notFound("Stock item response missing stockItemId");
                }

                log.debug("Found stock item: {} for product: {}", firstItem.getStockItemId(), productId.getValueAsString());
                return StockItemQueryResult.found(firstItem.getStockItemId());
            }

            log.warn("Stock management service returned unexpected response: status={}", response.getStatusCode());
            return StockItemQueryResult.notFound("Stock management service returned unexpected response");
        } catch (Exception e) {
            log.error("Failed to find stock item by product: productId={}", productId.getValueAsString(), e);
            return StockItemQueryResult.notFound("Stock management service error: " + e.getMessage());
        }
    }

    /**
     * DTO for stock-management-service stock items response wrapper.
     */
    private static class StockItemsByClassificationResponseDTO {
        private List<StockItemResponse> stockItems;

        public List<StockItemResponse> getStockItems() {
            return stockItems;
        }

        @SuppressWarnings("unused")
        public void setStockItems(List<StockItemResponse> stockItems) {
            this.stockItems = stockItems;
        }
    }

    /**
     * DTO for stock-management-service stock item response.
     */
    private static class StockItemResponse {
        private String stockItemId;
        private String productId;
        private String locationId;
        private Integer quantity;
        private Integer allocatedQuantity;
        private String expirationDate;
        private String classification;
        private String createdAt;
        private String lastModifiedAt;

        public String getStockItemId() {
            return stockItemId;
        }

        @SuppressWarnings("unused")
        public void setStockItemId(String stockItemId) {
            this.stockItemId = stockItemId;
        }

        public String getProductId() {
            return productId;
        }

        @SuppressWarnings("unused")
        public void setProductId(String productId) {
            this.productId = productId;
        }

        public String getLocationId() {
            return locationId;
        }

        @SuppressWarnings("unused")
        public void setLocationId(String locationId) {
            this.locationId = locationId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        @SuppressWarnings("unused")
        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Integer getAllocatedQuantity() {
            return allocatedQuantity;
        }

        @SuppressWarnings("unused")
        public void setAllocatedQuantity(Integer allocatedQuantity) {
            this.allocatedQuantity = allocatedQuantity;
        }

        public String getExpirationDate() {
            return expirationDate;
        }

        @SuppressWarnings("unused")
        public void setExpirationDate(String expirationDate) {
            this.expirationDate = expirationDate;
        }

        public String getClassification() {
            return classification;
        }

        @SuppressWarnings("unused")
        public void setClassification(String classification) {
            this.classification = classification;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        @SuppressWarnings("unused")
        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getLastModifiedAt() {
            return lastModifiedAt;
        }

        @SuppressWarnings("unused")
        public void setLastModifiedAt(String lastModifiedAt) {
            this.lastModifiedAt = lastModifiedAt;
        }
    }
}
