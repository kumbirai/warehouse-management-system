package com.ccbsa.wms.location.application.service.query.dto;

import java.math.BigDecimal;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: CheckLocationAvailabilityQuery
 * <p>
 * Query object for checking location availability and capacity.
 */
@Getter
@Builder
public final class CheckLocationAvailabilityQuery {
    private final LocationId locationId;
    private final BigDecimal requiredQuantity;
    private final TenantId tenantId;

    public CheckLocationAvailabilityQuery(LocationId locationId, BigDecimal requiredQuantity, TenantId tenantId) {
        if (locationId == null) {
            throw new IllegalArgumentException("LocationId is required");
        }
        if (requiredQuantity == null || requiredQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("RequiredQuantity must be positive");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        this.locationId = locationId;
        this.requiredQuantity = requiredQuantity;
        this.tenantId = tenantId;
    }
}

