package com.ccbsa.wms.picking.application.dto.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: OrderQueryResultDTO
 * <p>
 * API response DTO for order query results.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor and getter returns defensive copy")
public final class OrderQueryResultDTO {
    private final String orderNumber;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor")
    private final List<OrderLineItemQueryResultDTO> lineItems;

    public OrderQueryResultDTO(String orderNumber, List<OrderLineItemQueryResultDTO> lineItems) {
        this.orderNumber = orderNumber;
        this.lineItems = lineItems != null ? List.copyOf(lineItems) : List.of();
    }

    /**
     * Returns a defensive copy of the line items list to prevent external modification.
     *
     * @return unmodifiable copy of the line items list
     */
    public List<OrderLineItemQueryResultDTO> getLineItems() {
        return Collections.unmodifiableList(lineItems);
    }

    /**
     * Query Result DTO: OrderLineItemQueryResultDTO
     */
    @Getter
    @Builder
    public static final class OrderLineItemQueryResultDTO {
        private final String productId;
        private final Integer orderedQuantity;
        private final Integer pickedQuantity;
    }
}
