package com.ccbsa.wms.returns.dataaccess.adapter;

import java.util.List;
import java.util.Optional;

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
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.application.service.exception.PickingServiceException;
import com.ccbsa.wms.returns.application.service.port.service.PickingServicePort;
import com.ccbsa.wms.returns.application.service.port.service.dto.PickingOrderDetails;

import lombok.extern.slf4j.Slf4j;

/**
 * Service Adapter: PickingServiceAdapter
 * <p>
 * Implements PickingServicePort for fetching picking order details from picking-service.
 */
@Component
@Slf4j
public class PickingServiceAdapter implements PickingServicePort {
    private static final ParameterizedTypeReference<ApiResponse<PickingOrderResponse>> PICKING_ORDER_RESPONSE_TYPE =
            new ParameterizedTypeReference<ApiResponse<PickingOrderResponse>>() {
            };

    private final RestTemplate restTemplate;
    private final String pickingServiceUrl;

    public PickingServiceAdapter(RestTemplate restTemplate, @Value("${picking.service.url:http://picking-service}") String pickingServiceUrl) {
        this.restTemplate = restTemplate;
        this.pickingServiceUrl = pickingServiceUrl;
    }

    @Override
    public boolean isOrderPickingCompleted(OrderNumber orderNumber, TenantId tenantId) {
        Optional<PickingOrderDetails> details = getPickingOrderDetails(orderNumber, tenantId);
        if (details.isEmpty()) {
            log.debug("Order not found in picking service: orderNumber={}, tenantId={}", orderNumber.getValue(), tenantId.getValue());
            return false;
        }

        PickingOrderDetails orderDetails = details.get();
        // Check if all line items have picked quantity > 0
        boolean allPicked = orderDetails.getLineItems().stream().allMatch(item -> item.getPickedQuantity().isPositive());
        
        if (!allPicked) {
            log.debug("Order picking not completed: orderNumber={}, tenantId={}, lineItems={}", 
                    orderNumber.getValue(), tenantId.getValue(),
                    orderDetails.getLineItems().stream()
                            .map(item -> String.format("productId=%s, ordered=%s, picked=%s", 
                                    item.getProductId().getValue(), 
                                    item.getOrderedQuantity().getValue(), 
                                    item.getPickedQuantity().getValue()))
                            .collect(java.util.stream.Collectors.joining(", ")));
        }
        
        return allPicked;
    }

    @Override
    public Optional<PickingOrderDetails> getPickingOrderDetails(OrderNumber orderNumber, TenantId tenantId) {
        log.debug("Getting picking order details from picking-service: orderNumber={}, tenantId={}", orderNumber.getValue(), tenantId.getValue());

        try {
            String url = String.format("%s/api/v1/picking/orders/%s", pickingServiceUrl, orderNumber.getValue());
            log.debug("Calling picking service: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-Id", tenantId.getValue());

            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<ApiResponse<PickingOrderResponse>> response = restTemplate.exchange(url, HttpMethod.GET, entity, PICKING_ORDER_RESPONSE_TYPE);

            ApiResponse<PickingOrderResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                PickingOrderResponse orderResponse = responseBody.getData();
                List<PickingOrderDetails.PickingOrderLineItem> lineItems = orderResponse.getLineItems().stream()
                        .map(item -> new PickingOrderDetails.PickingOrderLineItem(ProductId.of(item.getProductId()), Quantity.of(item.getOrderedQuantity()),
                                Quantity.of(item.getPickedQuantity()))).collect(java.util.stream.Collectors.toList());

                PickingOrderDetails details = new PickingOrderDetails(OrderNumber.of(orderResponse.getOrderNumber()), lineItems);
                return Optional.of(details);
            }

            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Picking order not found: {}", orderNumber.getValue());
            return Optional.empty();
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Unauthorized access to picking-service: orderNumber={}, status={}, response={}", orderNumber.getValue(), e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new PickingServiceException("Picking service authentication failed. Please check service account configuration.", e);
        } catch (HttpServerErrorException e) {
            log.error("Picking service returned server error: orderNumber={}, status={}, response={}", orderNumber.getValue(), e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new PickingServiceException(
                    String.format("Picking service error (status %s): %s", e.getStatusCode(), e.getResponseBodyAsString() != null ? e.getResponseBodyAsString() : e.getMessage()),
                    e);
        } catch (IllegalArgumentException e) {
            // Handle LoadBalancer service discovery failures
            if (e.getMessage() != null && e.getMessage().contains("Service Instance cannot be null")) {
                log.error("Picking service not found in service registry (Eureka). Please ensure picking-service is running and registered with Eureka: orderNumber={}, "
                        + "serviceUrl={}", orderNumber.getValue(), pickingServiceUrl, e);
                throw new PickingServiceException(
                        "Picking service is not available. The service may not be running or not registered with service discovery. Please contact system administrator.",
                        e);
            }
            log.error("Invalid argument when calling picking-service: orderNumber={}, error={}", orderNumber.getValue(), e.getMessage(), e);
            throw new PickingServiceException(String.format("Picking service error: %s", e.getMessage()), e);
        } catch (RestClientException e) {
            log.error("Failed to get picking order details from picking-service: orderNumber={}, error={}", orderNumber.getValue(), e.getMessage(), e);
            throw new PickingServiceException("Picking service is temporarily unavailable. Please try again later.", e);
        } catch (Exception e) {
            log.error("Unexpected error getting picking order details: orderNumber={}", orderNumber.getValue(), e);
            throw new PickingServiceException(String.format("Picking service error: %s", e.getMessage()), e);
        }
    }

    /**
     * Response DTO for Picking Service
     */
    private static class PickingOrderResponse {
        private String orderNumber;
        private List<PickingOrderLineItemResponse> lineItems;

        public String getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
        }

        public List<PickingOrderLineItemResponse> getLineItems() {
            return lineItems;
        }

        public void setLineItems(List<PickingOrderLineItemResponse> lineItems) {
            this.lineItems = lineItems;
        }
    }

    /**
     * Response DTO for Picking Order Line Item
     */
    private static class PickingOrderLineItemResponse {
        private String productId;
        private Integer orderedQuantity;
        private Integer pickedQuantity;

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public Integer getOrderedQuantity() {
            return orderedQuantity;
        }

        public void setOrderedQuantity(Integer orderedQuantity) {
            this.orderedQuantity = orderedQuantity;
        }

        public Integer getPickedQuantity() {
            return pickedQuantity;
        }

        public void setPickedQuantity(Integer pickedQuantity) {
            this.pickedQuantity = pickedQuantity;
        }
    }
}
