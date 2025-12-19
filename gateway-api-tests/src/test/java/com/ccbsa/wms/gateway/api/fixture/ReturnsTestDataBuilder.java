package com.ccbsa.wms.gateway.api.fixture;

import java.time.LocalDate;
import java.util.List;

import com.ccbsa.wms.gateway.api.dto.CreateReturnOrderRequest;
import com.ccbsa.wms.gateway.api.dto.ReturnItem;

/**
 * Builder for creating return order test data.
 */
public class ReturnsTestDataBuilder {

    public static CreateReturnOrderRequest buildCreateReturnOrderRequest(String productId) {
        return CreateReturnOrderRequest.builder()
                .originalOrderId(TestData.orderId())
                .customerId(TestData.customerId())
                .items(List.of(
                        ReturnItem.builder()
                                .productId(productId)
                                .quantity(TestData.faker().number().numberBetween(1, 20))
                                .reason("DAMAGED")
                                .condition("DAMAGED")
                                .build()
                ))
                .returnDate(LocalDate.now())
                .build();
    }

    public static CreateReturnOrderRequest buildCreateReturnOrderRequestWithCondition(
            String productId, String condition) {
        return CreateReturnOrderRequest.builder()
                .originalOrderId(TestData.orderId())
                .customerId(TestData.customerId())
                .items(List.of(
                        ReturnItem.builder()
                                .productId(productId)
                                .quantity(TestData.faker().number().numberBetween(1, 20))
                                .reason(condition)
                                .condition(condition)
                                .build()
                ))
                .returnDate(LocalDate.now())
                .build();
    }
}

