package com.ccbsa.wms.integration.dataaccess.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.integration.application.service.port.service.D365ClientPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter: D365ODataClientAdapter
 * <p>
 * Implements D365ClientPort for OData API calls to D365 Finance and Operations.
 * <p>
 * This adapter handles:
 * - Creating return orders in D365
 * - Adjusting inventory in D365
 * - Processing financial reconciliation (credit notes, write-offs)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class D365ODataClientAdapter implements D365ClientPort {
    private final RestTemplate restTemplate;
    private final D365AuthenticationService authenticationService;

    @Value("${d365.odata.base-url:}")
    private String d365BaseUrl;

    @Override
    public Optional<String> createReturnOrder(ReturnOrderData returnOrderData, TenantId tenantId) {
        log.debug("Creating return order in D365: orderNumber={}, tenantId={}", returnOrderData.orderNumber().getValue(), tenantId.getValue());

        try {
            String accessToken = authenticationService.getAccessToken(tenantId.getValue());
            String url = String.format("%s/api/services/ReturnOrderService/CreateReturnOrder", d365BaseUrl);

            HttpHeaders headers = createHeaders(accessToken);
            ReturnOrderRequest request = buildReturnOrderRequest(returnOrderData);
            HttpEntity<ReturnOrderRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ReturnOrderResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, ReturnOrderResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ReturnOrderResponse responseBody = response.getBody();
                log.info("Created return order in D365: orderNumber={}, d365ReturnOrderId={}", returnOrderData.orderNumber().getValue(), responseBody.returnOrderId);
                return Optional.of(responseBody.returnOrderId);
            }

            log.warn("Failed to create return order in D365: orderNumber={}, status={}", returnOrderData.orderNumber().getValue(), response.getStatusCode());
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("Error creating return order in D365: orderNumber={}", returnOrderData.orderNumber().getValue(), e);
            throw new RuntimeException("Failed to create return order in D365", e);
        }
    }

    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private ReturnOrderRequest buildReturnOrderRequest(ReturnOrderData data) {
        ReturnOrderRequest request = new ReturnOrderRequest();
        request.setOrderNumber(data.orderNumber().getValue());
        request.setReturnReason(data.returnReason());
        request.setLineItems(data.lineItems().stream().map(item -> {
            ReturnOrderLineItemRequest lineItem = new ReturnOrderLineItemRequest();
            lineItem.setProductId(item.productId());
            lineItem.setQuantity(item.quantity());
            lineItem.setProductCondition(item.productCondition());
            lineItem.setReturnReason(item.returnReason());
            return lineItem;
        }).toList());
        return request;
    }

    @Override
    public boolean adjustInventory(InventoryAdjustmentData inventoryAdjustmentData, TenantId tenantId) {
        log.debug("Adjusting inventory in D365: orderNumber={}, tenantId={}", inventoryAdjustmentData.orderNumber().getValue(), tenantId.getValue());

        try {
            String accessToken = authenticationService.getAccessToken(tenantId.getValue());
            String url = String.format("%s/api/services/InventoryService/AdjustInventory", d365BaseUrl);

            HttpHeaders headers = createHeaders(accessToken);
            InventoryAdjustmentRequest request = buildInventoryAdjustmentRequest(inventoryAdjustmentData);
            HttpEntity<InventoryAdjustmentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.info("Adjusted inventory in D365: orderNumber={}", inventoryAdjustmentData.orderNumber().getValue());
                return true;
            }

            log.warn("Failed to adjust inventory in D365: orderNumber={}, status={}", inventoryAdjustmentData.orderNumber().getValue(), response.getStatusCode());
            return false;
        } catch (RestClientException e) {
            log.error("Error adjusting inventory in D365: orderNumber={}", inventoryAdjustmentData.orderNumber().getValue(), e);
            throw new RuntimeException("Failed to adjust inventory in D365", e);
        }
    }

    private InventoryAdjustmentRequest buildInventoryAdjustmentRequest(InventoryAdjustmentData data) {
        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest();
        request.setOrderNumber(data.orderNumber().getValue());
        request.setAdjustmentReason(data.adjustmentReason());
        request.setLineItems(data.lineItems().stream().map(item -> {
            InventoryAdjustmentLineItemRequest lineItem = new InventoryAdjustmentLineItemRequest();
            lineItem.setProductId(item.productId());
            lineItem.setQuantity(item.quantity());
            lineItem.setAdjustmentType(item.adjustmentType());
            return lineItem;
        }).toList());
        return request;
    }

    @Override
    public Optional<String> createCreditNote(CreditNoteData creditNoteData, TenantId tenantId) {
        log.debug("Creating credit note in D365: orderNumber={}, tenantId={}", creditNoteData.orderNumber().getValue(), tenantId.getValue());

        try {
            String accessToken = authenticationService.getAccessToken(tenantId.getValue());
            String url = String.format("%s/api/services/CreditNoteService/CreateCreditNote", d365BaseUrl);

            HttpHeaders headers = createHeaders(accessToken);
            CreditNoteRequest request = buildCreditNoteRequest(creditNoteData);
            HttpEntity<CreditNoteRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<CreditNoteResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, CreditNoteResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                CreditNoteResponse responseBody = response.getBody();
                log.info("Created credit note in D365: orderNumber={}, creditNoteId={}", creditNoteData.orderNumber().getValue(), responseBody.creditNoteId);
                return Optional.of(responseBody.creditNoteId);
            }

            log.warn("Failed to create credit note in D365: orderNumber={}, status={}", creditNoteData.orderNumber().getValue(), response.getStatusCode());
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("Error creating credit note in D365: orderNumber={}", creditNoteData.orderNumber().getValue(), e);
            throw new RuntimeException("Failed to create credit note in D365", e);
        }
    }

    private CreditNoteRequest buildCreditNoteRequest(CreditNoteData data) {
        CreditNoteRequest request = new CreditNoteRequest();
        request.setOrderNumber(data.orderNumber().getValue());
        request.setAmount(data.amount());
        request.setCurrencyCode(data.currencyCode());
        request.setReason(data.reason());
        return request;
    }

    @Override
    public boolean processWriteOff(WriteOffData writeOffData, TenantId tenantId) {
        log.debug("Processing write-off in D365: orderNumber={}, tenantId={}", writeOffData.orderNumber().getValue(), tenantId.getValue());

        try {
            String accessToken = authenticationService.getAccessToken(tenantId.getValue());
            String url = String.format("%s/api/services/WriteOffService/ProcessWriteOff", d365BaseUrl);

            HttpHeaders headers = createHeaders(accessToken);
            WriteOffRequest request = buildWriteOffRequest(writeOffData);
            HttpEntity<WriteOffRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.info("Processed write-off in D365: orderNumber={}", writeOffData.orderNumber().getValue());
                return true;
            }

            log.warn("Failed to process write-off in D365: orderNumber={}, status={}", writeOffData.orderNumber().getValue(), response.getStatusCode());
            return false;
        } catch (RestClientException e) {
            log.error("Error processing write-off in D365: orderNumber={}", writeOffData.orderNumber().getValue(), e);
            throw new RuntimeException("Failed to process write-off in D365", e);
        }
    }

    private WriteOffRequest buildWriteOffRequest(WriteOffData data) {
        WriteOffRequest request = new WriteOffRequest();
        request.setOrderNumber(data.orderNumber().getValue());
        request.setAmount(data.amount());
        request.setCurrencyCode(data.currencyCode());
        request.setReason(data.reason());
        return request;
    }

    // Request/Response DTOs for D365 API

    private static class ReturnOrderRequest {
        private String orderNumber;
        private String returnReason;
        private List<ReturnOrderLineItemRequest> lineItems;

        public String getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
        }

        public String getReturnReason() {
            return returnReason;
        }

        public void setReturnReason(String returnReason) {
            this.returnReason = returnReason;
        }

        public List<ReturnOrderLineItemRequest> getLineItems() {
            return lineItems;
        }

        public void setLineItems(List<ReturnOrderLineItemRequest> lineItems) {
            this.lineItems = lineItems;
        }
    }

    private static class ReturnOrderLineItemRequest {
        private String productId;
        private int quantity;
        private String productCondition;
        private String returnReason;

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public String getProductCondition() {
            return productCondition;
        }

        public void setProductCondition(String productCondition) {
            this.productCondition = productCondition;
        }

        public String getReturnReason() {
            return returnReason;
        }

        public void setReturnReason(String returnReason) {
            this.returnReason = returnReason;
        }
    }

    private static class ReturnOrderResponse {
        private String returnOrderId;

        public String getReturnOrderId() {
            return returnOrderId;
        }

        public void setReturnOrderId(String returnOrderId) {
            this.returnOrderId = returnOrderId;
        }
    }

    private static class InventoryAdjustmentRequest {
        private String orderNumber;
        private String adjustmentReason;
        private List<InventoryAdjustmentLineItemRequest> lineItems;

        public String getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
        }

        public String getAdjustmentReason() {
            return adjustmentReason;
        }

        public void setAdjustmentReason(String adjustmentReason) {
            this.adjustmentReason = adjustmentReason;
        }

        public List<InventoryAdjustmentLineItemRequest> getLineItems() {
            return lineItems;
        }

        public void setLineItems(List<InventoryAdjustmentLineItemRequest> lineItems) {
            this.lineItems = lineItems;
        }
    }

    private static class InventoryAdjustmentLineItemRequest {
        private String productId;
        private int quantity;
        private String adjustmentType;

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public String getAdjustmentType() {
            return adjustmentType;
        }

        public void setAdjustmentType(String adjustmentType) {
            this.adjustmentType = adjustmentType;
        }
    }

    private static class CreditNoteRequest {
        private String orderNumber;
        private java.math.BigDecimal amount;
        private String currencyCode;
        private String reason;

        public String getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
        }

        public java.math.BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(java.math.BigDecimal amount) {
            this.amount = amount;
        }

        public String getCurrencyCode() {
            return currencyCode;
        }

        public void setCurrencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    private static class CreditNoteResponse {
        private String creditNoteId;

        public String getCreditNoteId() {
            return creditNoteId;
        }

        public void setCreditNoteId(String creditNoteId) {
            this.creditNoteId = creditNoteId;
        }
    }

    private static class WriteOffRequest {
        private String orderNumber;
        private java.math.BigDecimal amount;
        private String currencyCode;
        private String reason;

        public String getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
        }

        public java.math.BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(java.math.BigDecimal amount) {
            this.amount = amount;
        }

        public String getCurrencyCode() {
            return currencyCode;
        }

        public void setCurrencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
