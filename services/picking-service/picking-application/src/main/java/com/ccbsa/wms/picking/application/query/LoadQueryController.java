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
import com.ccbsa.wms.picking.application.dto.query.ListOrdersByLoadQueryResultDTO;
import com.ccbsa.wms.picking.application.dto.query.LoadQueryResultDTO;
import com.ccbsa.wms.picking.application.service.query.GetLoadQueryHandler;
import com.ccbsa.wms.picking.application.service.query.ListOrdersByLoadQueryHandler;
import com.ccbsa.wms.picking.application.service.query.dto.GetLoadQuery;
import com.ccbsa.wms.picking.application.service.query.dto.ListOrdersByLoadQuery;
import com.ccbsa.wms.picking.application.service.query.dto.ListOrdersByLoadQueryResult;
import com.ccbsa.wms.picking.application.service.query.dto.LoadQueryResult;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller: LoadQueryController
 * <p>
 * Handles load query operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/picking/loads")
@Tag(name = "Load Queries", description = "Load query operations")
@RequiredArgsConstructor
public class LoadQueryController {
    private final GetLoadQueryHandler getLoadQueryHandler;
    private final ListOrdersByLoadQueryHandler listOrdersByLoadQueryHandler;

    @GetMapping("/{id}")
    @Operation(summary = "Get Load by ID", description = "Retrieves a load by ID")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'PICKING_MANAGER', 'OPERATOR', 'PICKING_CLERK', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<LoadQueryResultDTO>> getLoad(@PathVariable String id, @RequestHeader("X-Tenant-Id") String tenantId) {
        GetLoadQuery query = GetLoadQuery.builder().tenantId(com.ccbsa.common.domain.valueobject.TenantId.of(tenantId)).loadId(LoadId.of(id)).build();
        LoadQueryResult result = getLoadQueryHandler.handle(query);
        LoadQueryResultDTO dto = toLoadQueryResultDTO(result);
        return ApiResponseBuilder.ok(dto);
    }

    private LoadQueryResultDTO toLoadQueryResultDTO(LoadQueryResult result) {
        List<LoadQueryResultDTO.OrderQueryResultDTO> orderDTOs = result.getOrders() != null ? result.getOrders().stream().map(orderResult -> {
            List<LoadQueryResultDTO.OrderLineItemQueryResultDTO> lineItemDTOs = orderResult.getLineItems() != null ? orderResult.getLineItems().stream()
                    .map(lineItemResult -> LoadQueryResultDTO.OrderLineItemQueryResultDTO.builder().lineItemId(lineItemResult.getLineItemId())
                            .productCode(lineItemResult.getProductCode()).productDescription(lineItemResult.getProductDescription()).quantity(lineItemResult.getQuantity())
                            .notes(lineItemResult.getNotes()).build()).collect(Collectors.toList()) : List.of();

            // Create defensive copy of line items list for builder
            return LoadQueryResultDTO.OrderQueryResultDTO.builder().orderId(orderResult.getOrderId()).orderNumber(orderResult.getOrderNumber())
                    .customerCode(orderResult.getCustomerCode()).customerName(orderResult.getCustomerName()).priority(orderResult.getPriority()).status(orderResult.getStatus())
                    .lineItems(new java.util.ArrayList<>(lineItemDTOs)).build();
        }).collect(Collectors.toList()) : List.of();

        // Create defensive copy of orders list for builder
        return LoadQueryResultDTO.builder().id(result.getId().getValueAsString()).loadNumber(result.getLoadNumber().getValue()).status(result.getStatus().name())
                .createdAt(result.getCreatedAt()).plannedAt(result.getPlannedAt()).orders(new java.util.ArrayList<>(orderDTOs)).build();
    }

    @GetMapping("/{id}/orders")
    @Operation(summary = "List Orders by Load", description = "Lists orders for a specific load")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'PICKING_MANAGER', 'OPERATOR', 'PICKING_CLERK', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<ListOrdersByLoadQueryResultDTO>> listOrdersByLoad(@PathVariable String id, @RequestHeader("X-Tenant-Id") String tenantId) {
        ListOrdersByLoadQuery query = ListOrdersByLoadQuery.builder().tenantId(com.ccbsa.common.domain.valueobject.TenantId.of(tenantId)).loadId(LoadId.of(id)).build();
        ListOrdersByLoadQueryResult result = listOrdersByLoadQueryHandler.handle(query);
        ListOrdersByLoadQueryResultDTO dto = toListOrdersByLoadQueryResultDTO(result);
        return ApiResponseBuilder.ok(dto);
    }

    private ListOrdersByLoadQueryResultDTO toListOrdersByLoadQueryResultDTO(ListOrdersByLoadQueryResult result) {
        List<ListOrdersByLoadQueryResultDTO.OrderQueryResultDTO> orderDTOs = result.getOrders() != null ? result.getOrders().stream().map(orderResult -> {
            List<ListOrdersByLoadQueryResultDTO.OrderLineItemQueryResultDTO> lineItemDTOs = orderResult.getLineItems() != null ? orderResult.getLineItems().stream()
                    .map(lineItemResult -> ListOrdersByLoadQueryResultDTO.OrderLineItemQueryResultDTO.builder().lineItemId(lineItemResult.getLineItemId())
                            .productCode(lineItemResult.getProductCode()).productDescription(lineItemResult.getProductDescription()).quantity(lineItemResult.getQuantity())
                            .notes(lineItemResult.getNotes()).build()).collect(Collectors.toList()) : List.of();

            // Create defensive copy of line items list for builder
            return ListOrdersByLoadQueryResultDTO.OrderQueryResultDTO.builder().orderId(orderResult.getOrderId()).orderNumber(orderResult.getOrderNumber())
                    .customerCode(orderResult.getCustomerCode()).customerName(orderResult.getCustomerName()).priority(orderResult.getPriority()).status(orderResult.getStatus())
                    .lineItems(new java.util.ArrayList<>(lineItemDTOs)).build();
        }).collect(Collectors.toList()) : List.of();

        // Create defensive copy of orders list for builder
        return ListOrdersByLoadQueryResultDTO.builder().loadId(result.getLoadId()).orders(new java.util.ArrayList<>(orderDTOs)).build();
    }
}
