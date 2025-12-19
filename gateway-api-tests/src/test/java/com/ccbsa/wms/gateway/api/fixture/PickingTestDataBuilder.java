package com.ccbsa.wms.gateway.api.fixture;

import com.ccbsa.wms.gateway.api.dto.CreatePickingTaskRequest;
import com.ccbsa.wms.gateway.api.dto.PickingItem;

import java.time.LocalDate;
import java.util.List;

/**
 * Builder for creating picking task test data.
 */
public class PickingTestDataBuilder {

    public static CreatePickingTaskRequest buildCreatePickingTaskRequest(String productId, String locationId) {
        return CreatePickingTaskRequest.builder()
                .orderId(TestData.orderId())
                .items(List.of(
                    PickingItem.builder()
                        .productId(productId)
                        .quantity(TestData.faker().number().numberBetween(10, 100))
                        .locationId(locationId)
                        .build()
                ))
                .priority("HIGH")
                .dueDate(LocalDate.now().plusDays(7))
                .build();
    }

    public static CreatePickingTaskRequest buildCreatePickingTaskRequestWithQuantity(
            String productId, String locationId, int quantity) {
        return CreatePickingTaskRequest.builder()
                .orderId(TestData.orderId())
                .items(List.of(
                    PickingItem.builder()
                        .productId(productId)
                        .quantity(quantity)
                        .locationId(locationId)
                        .build()
                ))
                .priority("HIGH")
                .dueDate(LocalDate.now().plusDays(7))
                .build();
    }
}

