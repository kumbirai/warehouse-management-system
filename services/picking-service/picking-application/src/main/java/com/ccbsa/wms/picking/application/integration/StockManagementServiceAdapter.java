package com.ccbsa.wms.picking.application.integration;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.picking.application.service.exception.StockManagementServiceException;
import com.ccbsa.wms.picking.application.service.port.service.StockManagementServicePort;
import com.ccbsa.wms.picking.application.service.port.service.dto.StockAvailabilityInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * Service Adapter: StockManagementServiceAdapter
 * <p>
 * Implements StockManagementServicePort for stock availability queries (FEFO).
 */
@Component
@Slf4j
public class StockManagementServiceAdapter implements StockManagementServicePort {
    private static final ParameterizedTypeReference<ApiResponse<StockAvailabilityResponse>> STOCK_RESPONSE_TYPE =
            new ParameterizedTypeReference<ApiResponse<StockAvailabilityResponse>>() {
            };

    private final RestTemplate restTemplate;
    private final String stockManagementServiceUrl;

    public StockManagementServiceAdapter(RestTemplate restTemplate, @Value("${stock-management.service.url:http://stock-management-service}") String stockManagementServiceUrl) {
        this.restTemplate = restTemplate;
        this.stockManagementServiceUrl = stockManagementServiceUrl;
    }

    @Override
    public List<StockAvailabilityInfo> queryAvailableStockByFEFO(String productCode, int quantity) {
        log.debug("Querying available stock by FEFO for product: {}, quantity: {}", productCode, quantity);

        try {
            String url = String.format("%s/api/v1/stock-management/stock/query-availability-fefo", stockManagementServiceUrl);
            log.debug("Calling stock management service: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            StockAvailabilityFefoRequest request = new StockAvailabilityFefoRequest();
            request.setProductCode(productCode);
            request.setQuantity(quantity);

            HttpEntity<StockAvailabilityFefoRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<ApiResponse<StockAvailabilityFefoResponse>> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, new ParameterizedTypeReference<ApiResponse<StockAvailabilityFefoResponse>>() {
                    });

            ApiResponse<StockAvailabilityFefoResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                StockAvailabilityFefoResponse stockResponse = responseBody.getData();
                return convertToStockAvailabilityInfoList(stockResponse);
            }

            log.warn("Stock management service returned unexpected response");
            return List.of();
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Unauthorized access to stock-management-service: productCode={}, status={}, response={}", productCode, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new StockManagementServiceException("Stock management service authentication failed. Please check service account configuration.", e);
        } catch (HttpServerErrorException e) {
            log.error("Stock management service returned server error: productCode={}, status={}, response={}", productCode, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new StockManagementServiceException(String.format("Stock management service error (status %s): %s", e.getStatusCode(),
                    e.getResponseBodyAsString() != null ? e.getResponseBodyAsString() : e.getMessage()), e);
        } catch (IllegalArgumentException e) {
            // Handle LoadBalancer service discovery failures
            if (e.getMessage() != null && e.getMessage().contains("Service Instance cannot be null")) {
                log.error(
                        "Stock management service not found in service registry (Eureka). Please ensure stock-management-service is running and registered with Eureka: "
                                + "productCode={}, "
                                + "serviceUrl={}", productCode, stockManagementServiceUrl, e);
                throw new StockManagementServiceException(
                        "Stock management service is not available. The service may not be running or not registered with service discovery. Please contact system administrator.",
                        e);
            }
            log.error("Invalid argument when calling stock-management-service: productCode={}, error={}", productCode, e.getMessage(), e);
            throw new StockManagementServiceException(String.format("Stock management service error: %s", e.getMessage()), e);
        } catch (RestClientException e) {
            log.error("Failed to query stock availability by FEFO from stock-management-service: productCode={}, error={}", productCode, e.getMessage(), e);
            throw new StockManagementServiceException("Stock management service is temporarily unavailable. Please try again later.", e);
        }
    }

    private List<StockAvailabilityInfo> convertToStockAvailabilityInfoList(StockAvailabilityFefoResponse response) {
        if (response == null || response.getStockItems() == null) {
            return List.of();
        }

        return response.getStockItems().stream()
                .map(item -> StockAvailabilityInfo.builder().locationId(item.getLocationId()).productCode(response.getProductCode()).availableQuantity(item.getAvailableQuantity())
                        .expirationDate(item.getExpirationDate()).stockItemId(item.getStockItemId()).build()).toList();
    }

    @Override
    public Map<String, List<StockAvailabilityInfo>> queryAvailableStockForProducts(Map<String, Integer> productQuantities) {
        log.debug("Querying available stock for {} products", productQuantities.size());

        try {
            // Build request
            StockAvailabilityRequest request = new StockAvailabilityRequest();
            request.setProductQuantities(productQuantities);

            String url = String.format("%s/api/v1/stock-management/stock/query-availability", stockManagementServiceUrl);
            log.debug("Calling stock management service: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            HttpEntity<StockAvailabilityRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<ApiResponse<StockAvailabilityResponse>> response = restTemplate.exchange(url, HttpMethod.POST, entity, STOCK_RESPONSE_TYPE);

            ApiResponse<StockAvailabilityResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                StockAvailabilityResponse stockResponse = responseBody.getData();
                return convertToStockAvailabilityInfo(stockResponse);
            }

            log.warn("Stock management service returned unexpected response");
            return Map.of();
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Unauthorized access to stock-management-service: productQuantities={}, status={}, response={}", productQuantities.keySet(), e.getStatusCode(),
                    e.getResponseBodyAsString(), e);
            throw new StockManagementServiceException("Stock management service authentication failed. Please check service account configuration.", e);
        } catch (HttpServerErrorException e) {
            log.error("Stock management service returned server error: productQuantities={}, status={}, response={}", productQuantities.keySet(), e.getStatusCode(),
                    e.getResponseBodyAsString(), e);
            throw new StockManagementServiceException(String.format("Stock management service error (status %s): %s", e.getStatusCode(),
                    e.getResponseBodyAsString() != null ? e.getResponseBodyAsString() : e.getMessage()), e);
        } catch (IllegalArgumentException e) {
            // Handle LoadBalancer service discovery failures
            if (e.getMessage() != null && e.getMessage().contains("Service Instance cannot be null")) {
                log.error("Stock management service not found in service registry (Eureka). Please ensure stock-management-service is running and registered with Eureka: "
                        + "productQuantities={}, serviceUrl={}", productQuantities.keySet(), stockManagementServiceUrl, e);
                throw new StockManagementServiceException(
                        "Stock management service is not available. The service may not be running or not registered with service discovery. Please contact system administrator.",
                        e);
            }
            log.error("Invalid argument when calling stock-management-service: productQuantities={}, error={}", productQuantities.keySet(), e.getMessage(), e);
            throw new StockManagementServiceException(String.format("Stock management service error: %s", e.getMessage()), e);
        } catch (RestClientException e) {
            log.error("Failed to query stock availability from stock-management-service: productQuantities={}, error={}", productQuantities.keySet(), e.getMessage(), e);
            throw new StockManagementServiceException("Stock management service is temporarily unavailable. Please try again later.", e);
        }
    }

    private Map<String, List<StockAvailabilityInfo>> convertToStockAvailabilityInfo(StockAvailabilityResponse response) {
        if (response == null || response.getStockByProduct() == null) {
            return Map.of();
        }

        return response.getStockByProduct().entrySet().stream().filter(entry -> entry.getKey() != null).collect(Collectors.toMap(Map.Entry::getKey,
                entry -> entry.getValue().stream()
                        .map(item -> StockAvailabilityInfo.builder().locationId(item.getLocationId()).productCode(entry.getKey()).availableQuantity(item.getAvailableQuantity())
                                .expirationDate(item.getExpirationDate()).stockItemId(item.getStockItemId()).build()).toList()));
    }

    /**
     * Request DTO for Stock Availability Query
     */
    private static class StockAvailabilityRequest {
        private Map<String, Integer> productQuantities;

        public Map<String, Integer> getProductQuantities() {
            return productQuantities;
        }

        public void setProductQuantities(Map<String, Integer> productQuantities) {
            this.productQuantities = productQuantities;
        }
    }

    /**
     * Response DTO for Stock Availability Query
     */
    private static class StockAvailabilityResponse {
        private Map<String, List<StockItemDTO>> stockByProduct;

        public Map<String, List<StockItemDTO>> getStockByProduct() {
            return stockByProduct;
        }

        public void setStockByProduct(Map<String, List<StockItemDTO>> stockByProduct) {
            this.stockByProduct = stockByProduct;
        }
    }

    /**
     * Stock Item DTO
     */
    private static class StockItemDTO {
        private String locationId;
        private int availableQuantity;
        private LocalDate expirationDate;
        private String stockItemId;

        public String getLocationId() {
            return locationId;
        }

        public void setLocationId(String locationId) {
            this.locationId = locationId;
        }

        public int getAvailableQuantity() {
            return availableQuantity;
        }

        public void setAvailableQuantity(int availableQuantity) {
            this.availableQuantity = availableQuantity;
        }

        public LocalDate getExpirationDate() {
            return expirationDate;
        }

        public void setExpirationDate(LocalDate expirationDate) {
            this.expirationDate = expirationDate;
        }

        public String getStockItemId() {
            return stockItemId;
        }

        public void setStockItemId(String stockItemId) {
            this.stockItemId = stockItemId;
        }
    }

    /**
     * Request DTO for Stock Availability FEFO Query
     */
    private static class StockAvailabilityFefoRequest {
        private String productCode;
        private int quantity;

        public String getProductCode() {
            return productCode;
        }

        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }

    /**
     * Response DTO for Stock Availability FEFO Query
     */
    private static class StockAvailabilityFefoResponse {
        private String productCode;
        private List<StockItemDTO> stockItems;

        public String getProductCode() {
            return productCode;
        }

        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        public List<StockItemDTO> getStockItems() {
            return stockItems;
        }

        public void setStockItems(List<StockItemDTO> stockItems) {
            this.stockItems = stockItems;
        }
    }
}
