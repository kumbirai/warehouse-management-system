package com.ccbsa.wms.picking.application.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.application.dto.query.OrderQueryResultDTO;
import com.ccbsa.wms.picking.application.service.query.GetOrderQueryHandler;
import com.ccbsa.wms.picking.application.service.query.dto.GetOrderQuery;
import com.ccbsa.wms.picking.application.service.query.dto.OrderQueryResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller: OrderQueryController
 * <p>
 * Handles order query operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/picking/orders")
@Tag(name = "Order Queries", description = "Order query operations")
@RequiredArgsConstructor
public class OrderQueryController {
    private final GetOrderQueryHandler getOrderQueryHandler;

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Get Order by Order Number", description = "Retrieves an order by order number with picking details")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'PICKING_MANAGER', 'OPERATOR', 'PICKING_CLERK', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<OrderQueryResultDTO>> getOrder(@PathVariable String orderNumber, @RequestHeader("X-Tenant-Id") String tenantId) {
        log.debug("Received get order request for orderNumber: {}, tenant: {}", orderNumber, tenantId);

        GetOrderQuery query = GetOrderQuery.builder()
                .tenantId(TenantId.of(tenantId))
                .orderNumber(OrderNumber.of(orderNumber))
                .build();

        OrderQueryResult result = getOrderQueryHandler.handle(query);
        OrderQueryResultDTO dto = toOrderQueryResultDTO(result);

        return ApiResponseBuilder.ok(dto);
    }

    private OrderQueryResultDTO toOrderQueryResultDTO(OrderQueryResult result) {
        List<OrderQueryResultDTO.OrderLineItemQueryResultDTO> lineItemDTOs = result.getLineItems().stream()
                .map(lineItem -> OrderQueryResultDTO.OrderLineItemQueryResultDTO.builder()
                        .productId(lineItem.getProductId())
                        .orderedQuantity(lineItem.getOrderedQuantity())
                        .pickedQuantity(lineItem.getPickedQuantity())
                        .build())
                .collect(Collectors.toList());

        return OrderQueryResultDTO.builder()
                .orderNumber(result.getOrderNumber())
                .lineItems(new java.util.ArrayList<>(lineItemDTOs))
                .build();
    }
}
