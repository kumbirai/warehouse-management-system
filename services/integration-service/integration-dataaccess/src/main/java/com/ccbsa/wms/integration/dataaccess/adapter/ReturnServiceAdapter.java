package com.ccbsa.wms.integration.dataaccess.adapter;

import java.util.List;
import java.util.Optional;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.ProductCondition;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.ReturnId;
import com.ccbsa.common.domain.valueobject.ReturnLineItemId;
import com.ccbsa.common.domain.valueobject.ReturnStatus;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.integration.application.service.port.service.ReturnServicePort;

import lombok.extern.slf4j.Slf4j;

/**
 * Service Adapter: ReturnServiceAdapter
 * <p>
 * Implements ReturnServicePort for fetching return details from returns-service.
 */
@Component
@Slf4j
public class ReturnServiceAdapter implements ReturnServicePort {
    private static final ParameterizedTypeReference<ApiResponse<ReturnResponse>> RETURN_RESPONSE_TYPE = new ParameterizedTypeReference<ApiResponse<ReturnResponse>>() {
    };

    private final RestTemplate restTemplate;
    private final String returnsServiceUrl;

    public ReturnServiceAdapter(RestTemplate restTemplate, @Value("${returns.service.url:http://returns-service}") String returnsServiceUrl) {
        this.restTemplate = restTemplate;
        this.returnsServiceUrl = returnsServiceUrl;
    }

    @Override
    public Optional<ReturnDetails> getReturnDetails(ReturnId returnId, TenantId tenantId) {
        log.debug("Getting return details from returns-service: returnId={}, tenantId={}", returnId.getValueAsString(), tenantId.getValue());

        try {
            String url = String.format("%s/api/v1/returns/%s", returnsServiceUrl, returnId.getValueAsString());
            log.debug("Calling returns service: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-Id", tenantId.getValue());

            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<ApiResponse<ReturnResponse>> response = restTemplate.exchange(url, HttpMethod.GET, entity, RETURN_RESPONSE_TYPE);

            ApiResponse<ReturnResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                ReturnResponse returnResponse = responseBody.getData();
                List<ReturnLineItemDetails> lineItems = returnResponse.getLineItems().stream()
                        .map(item -> new ReturnLineItemDetails(ReturnLineItemId.of(item.getLineItemId()), ProductId.of(item.getProductId()), item.getReturnedQuantity(),
                                item.getProductCondition() != null ? ProductCondition.valueOf(item.getProductCondition()) : null)).collect(Collectors.toList());

                ReturnDetails details = new ReturnDetails(ReturnId.of(returnResponse.getReturnId()), OrderNumber.of(returnResponse.getOrderNumber()),
                        ReturnStatus.valueOf(returnResponse.getStatus()), lineItems);
                return Optional.of(details);
            }

            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Return not found: {}", returnId.getValueAsString());
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("Failed to get return details: returnId={}", returnId.getValueAsString(), e);
            throw new RuntimeException("Returns service is temporarily unavailable", e);
        }
    }

    /**
     * Response DTO for Returns Service
     */
    private static class ReturnResponse {
        private String returnId;
        private String orderNumber;
        private String status;
        private List<ReturnLineItemResponse> lineItems;

        public String getReturnId() {
            return returnId;
        }

        public void setReturnId(String returnId) {
            this.returnId = returnId;
        }

        public String getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<ReturnLineItemResponse> getLineItems() {
            return lineItems;
        }

        public void setLineItems(List<ReturnLineItemResponse> lineItems) {
            this.lineItems = lineItems;
        }
    }

    /**
     * Response DTO for Return Line Item
     */
    private static class ReturnLineItemResponse {
        private String lineItemId;
        private String productId;
        private Integer returnedQuantity;
        private String productCondition;

        public String getLineItemId() {
            return lineItemId;
        }

        public void setLineItemId(String lineItemId) {
            this.lineItemId = lineItemId;
        }

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public Integer getReturnedQuantity() {
            return returnedQuantity;
        }

        public void setReturnedQuantity(Integer returnedQuantity) {
            this.returnedQuantity = returnedQuantity;
        }

        public String getProductCondition() {
            return productCondition;
        }

        public void setProductCondition(String productCondition) {
            this.productCondition = productCondition;
        }
    }
}
