package com.ccbsa.wms.gateway.api.fixture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Test data builder for Consignment entities.
 * Provides fluent API for creating consignment test data.
 */
@Slf4j
public final class ConsignmentTestDataBuilder {
    private String consignmentReference;
    private String warehouseId;
    private LocalDateTime receivedAt;
    private String receivedBy;
    private List<Map<String, Object>> lineItems;

    private ConsignmentTestDataBuilder() {
        this.lineItems = new ArrayList<>();
        this.receivedAt = LocalDateTime.now();
    }

    /**
     * Creates a default consignment with all required fields.
     *
     * @return Map with default consignment data
     */
    public static Map<String, Object> createDefault() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return builder()
                .consignmentReference("CONS-TEST-" + timestamp)
                .warehouseId("WH-001")
                .receivedAt(LocalDateTime.now())
                .receivedBy("Test User")
                .lineItem("PROD-TEST-001", 100, null, null)
                .build();
    }

    /**
     * Builds a map representing the consignment creation request.
     *
     * @return Map with consignment data
     */
    public Map<String, Object> build() {
        Map<String, Object> request = new HashMap<>();
        if (consignmentReference != null) {
            request.put("consignmentReference", consignmentReference);
        }
        if (warehouseId != null) {
            request.put("warehouseId", warehouseId);
        }
        if (receivedAt != null) {
            request.put("receivedAt", receivedAt.toString());
        }
        if (receivedBy != null) {
            request.put("receivedBy", receivedBy);
        }
        if (!lineItems.isEmpty()) {
            request.put("lineItems", lineItems);
        }
        return request;
    }

    /**
     * Adds a line item to the consignment.
     *
     * @param productCode    Product code
     * @param quantity       Quantity
     * @param expirationDate Expiration date (optional)
     * @param batchNumber    Batch number (optional)
     * @return Builder instance
     */
    public ConsignmentTestDataBuilder lineItem(
            String productCode,
            Integer quantity,
            LocalDate expirationDate,
            String batchNumber) {
        Map<String, Object> lineItem = new HashMap<>();
        lineItem.put("productCode", productCode);
        lineItem.put("quantity", quantity);
        if (expirationDate != null) {
            lineItem.put("expirationDate", expirationDate.toString());
        }
        if (batchNumber != null) {
            lineItem.put("batchNumber", batchNumber);
        }
        this.lineItems.add(lineItem);
        return this;
    }

    public ConsignmentTestDataBuilder receivedBy(String receivedBy) {
        this.receivedBy = receivedBy;
        return this;
    }

    public ConsignmentTestDataBuilder receivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
        return this;
    }

    public ConsignmentTestDataBuilder warehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
        return this;
    }

    public ConsignmentTestDataBuilder consignmentReference(String consignmentReference) {
        this.consignmentReference = consignmentReference;
        return this;
    }

    public static ConsignmentTestDataBuilder builder() {
        return new ConsignmentTestDataBuilder();
    }

    /**
     * Creates a CSV content string for testing CSV upload.
     *
     * @param consignmentReference Consignment reference
     * @param productCode          Product code
     * @param quantity             Quantity
     * @param warehouseId          Warehouse ID
     * @return CSV content string
     */
    public static String createCsvContent(
            String consignmentReference,
            String productCode,
            Integer quantity,
            String warehouseId) {
        return String.format(
                "ConsignmentReference,ProductCode,Quantity,ReceivedDate,WarehouseId\n%s,%s,%d,%s,%s",
                consignmentReference,
                productCode,
                quantity,
                LocalDateTime.now().toString(),
                warehouseId);
    }

    /**
     * Creates a CSV content string with multiple line items.
     *
     * @param consignmentReference Consignment reference
     * @param lineItems            List of line items (productCode, quantity, warehouseId)
     * @return CSV content string
     */
    public static String createCsvContentWithLineItems(
            String consignmentReference,
            List<Map<String, Object>> lineItems) {
        StringBuilder csv = new StringBuilder();
        csv.append("ConsignmentReference,ProductCode,Quantity,ReceivedDate,WarehouseId");
        if (!lineItems.isEmpty() && lineItems.get(0).containsKey("expirationDate")) {
            csv.append(",ExpirationDate");
        }
        if (!lineItems.isEmpty() && lineItems.get(0).containsKey("batchNumber")) {
            csv.append(",BatchNumber");
        }
        csv.append("\n");

        String receivedDate = LocalDateTime.now().toString();
        for (Map<String, Object> item : lineItems) {
            csv.append(consignmentReference)
                    .append(",")
                    .append(item.get("productCode"))
                    .append(",")
                    .append(item.get("quantity"))
                    .append(",")
                    .append(receivedDate)
                    .append(",")
                    .append(item.get("warehouseId"));
            if (item.containsKey("expirationDate")) {
                csv.append(",").append(item.get("expirationDate"));
            }
            if (item.containsKey("batchNumber")) {
                csv.append(",").append(item.get("batchNumber"));
            }
            csv.append("\n");
        }
        return csv.toString();
    }
}

