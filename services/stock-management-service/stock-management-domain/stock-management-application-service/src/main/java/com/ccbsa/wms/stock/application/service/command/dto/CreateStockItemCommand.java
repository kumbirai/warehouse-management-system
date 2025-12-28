package com.ccbsa.wms.stock.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.ExpirationDate;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: CreateStockItemCommand
 * <p>
 * Command object for creating a new stock item.
 */
@Getter
@Builder
@AllArgsConstructor
public final class CreateStockItemCommand {
    private final TenantId tenantId;
    private final ProductId productId;
    private final Quantity quantity;
    private final ExpirationDate expirationDate; // May be null for non-perishable
    private final LocationId locationId; // May be null initially
    private final ConsignmentId consignmentId; // Reference to source consignment

    /**
     * Static factory method with validation.
     */
    public static CreateStockItemCommand of(TenantId tenantId, ProductId productId, Quantity quantity, ExpirationDate expirationDate, LocationId locationId,
                                            ConsignmentId consignmentId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (productId == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity is required");
        }
        return CreateStockItemCommand.builder().tenantId(tenantId).productId(productId).quantity(quantity).expirationDate(expirationDate).locationId(locationId)
                .consignmentId(consignmentId).build();
    }
}

