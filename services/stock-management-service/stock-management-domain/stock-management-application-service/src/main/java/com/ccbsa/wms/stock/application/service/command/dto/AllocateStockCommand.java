package com.ccbsa.wms.stock.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.AllocationType;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Command DTO: AllocateStockCommand
 * <p>
 * Command for allocating stock for picking orders or reservations.
 */
@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public final class AllocateStockCommand {
    private final TenantId tenantId;
    private final ProductId productId;
    private final LocationId locationId; // Optional - null for FEFO allocation
    private final Quantity quantity;
    private final AllocationType allocationType;
    private final String referenceId; // Order ID, picking list ID, etc.
    private final UserId userId;
    private final String notes;
}

