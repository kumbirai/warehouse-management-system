package com.ccbsa.wms.returns.application.service.port.service.dto;

import java.util.List;

import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;

/**
 * DTO: PickingOrderDetails
 * <p>
 * Represents picking order details fetched from Picking Service.
 */
public class PickingOrderDetails {
    private final OrderNumber orderNumber;
    private final List<PickingOrderLineItem> lineItems;

    public PickingOrderDetails(OrderNumber orderNumber, List<PickingOrderLineItem> lineItems) {
        this.orderNumber = orderNumber;
        this.lineItems = lineItems != null ? List.copyOf(lineItems) : List.of();
    }

    public OrderNumber getOrderNumber() {
        return orderNumber;
    }

    public List<PickingOrderLineItem> getLineItems() {
        return lineItems;
    }

    /**
     * DTO: PickingOrderLineItem
     * <p>
     * Represents a line item in a picking order.
     */
    public static class PickingOrderLineItem {
        private final ProductId productId;
        private final Quantity orderedQuantity;
        private final Quantity pickedQuantity;

        public PickingOrderLineItem(ProductId productId, Quantity orderedQuantity, Quantity pickedQuantity) {
            this.productId = productId;
            this.orderedQuantity = orderedQuantity;
            this.pickedQuantity = pickedQuantity;
        }

        public ProductId getProductId() {
            return productId;
        }

        public Quantity getOrderedQuantity() {
            return orderedQuantity;
        }

        public Quantity getPickedQuantity() {
            return pickedQuantity;
        }
    }
}
