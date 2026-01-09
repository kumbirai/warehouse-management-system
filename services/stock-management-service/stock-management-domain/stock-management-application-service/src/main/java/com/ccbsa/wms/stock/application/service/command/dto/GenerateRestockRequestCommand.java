package com.ccbsa.wms.stock.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.BigDecimalQuantity;
import com.ccbsa.common.domain.valueobject.MaximumQuantity;
import com.ccbsa.common.domain.valueobject.MinimumQuantity;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: GenerateRestockRequestCommand
 * <p>
 * Command object for generating a restock request.
 */
@Getter
@Builder
public final class GenerateRestockRequestCommand {
    private final TenantId tenantId;
    private final ProductId productId;
    private final LocationId locationId;
    private final BigDecimalQuantity currentQuantity;
    private final MinimumQuantity minimumQuantity;
    private final MaximumQuantity maximumQuantity;
}
