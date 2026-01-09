package com.ccbsa.wms.gateway.picking;

import java.util.ArrayList;
import java.util.List;

import com.ccbsa.wms.gateway.api.dto.CreatePickingListRequest;
import com.ccbsa.wms.gateway.api.dto.LoadRequest;
import com.ccbsa.wms.gateway.api.dto.OrderLineItemRequest;
import com.ccbsa.wms.gateway.api.dto.OrderRequest;
import com.ccbsa.wms.gateway.api.fixture.TestData;

/**
 * Test Data Builder for Picking List tests.
 */
public class PickingListTestDataBuilder {

    public static CreatePickingListRequest buildCreatePickingListRequest() {
        return CreatePickingListRequest.builder()
                .loads(List.of(buildLoadRequest()))
                .notes("Test picking list")
                .build();
    }

    public static CreatePickingListRequest buildCreatePickingListRequestWithMultipleOrders() {
        List<LoadRequest> loads = new ArrayList<>();
        LoadRequest load1 = buildLoadRequest("LOAD-001");
        LoadRequest load2 = buildLoadRequest("LOAD-002");
        loads.add(load1);
        loads.add(load2);
        
        return CreatePickingListRequest.builder()
                .loads(loads)
                .notes("Test picking list with multiple loads")
                .build();
    }

    public static LoadRequest buildLoadRequest() {
        return buildLoadRequest("LOAD-" + TestData.faker().code().ean8());
    }

    public static LoadRequest buildLoadRequest(String loadNumber) {
        return LoadRequest.builder()
                .loadNumber(loadNumber)
                .orders(List.of(buildOrderRequest()))
                .build();
    }

    public static OrderRequest buildOrderRequest() {
        return buildOrderRequest("ORD-" + TestData.faker().code().ean8());
    }

    public static OrderRequest buildOrderRequest(String orderNumber) {
        return OrderRequest.builder()
                .orderNumber(orderNumber)
                .customerCode("CUST-" + TestData.faker().code().ean8())
                .customerName(TestData.faker().company().name())
                .priority("HIGH")
                .lineItems(List.of(buildOrderLineItemRequest()))
                .build();
    }

    public static OrderLineItemRequest buildOrderLineItemRequest() {
        return buildOrderLineItemRequest("PROD-" + TestData.faker().code().ean8(), 10);
    }

    public static OrderLineItemRequest buildOrderLineItemRequest(String productCode, int quantity) {
        return OrderLineItemRequest.builder()
                .productCode(productCode)
                .quantity(quantity)
                .notes("Test line item")
                .build();
    }

    public static CreatePickingListRequest buildInvalidPickingListRequest() {
        // Missing required fields
        return CreatePickingListRequest.builder()
                .loads(List.of())
                .build();
    }
}
