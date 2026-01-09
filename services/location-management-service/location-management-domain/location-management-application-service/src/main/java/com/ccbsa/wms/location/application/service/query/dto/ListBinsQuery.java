package com.ccbsa.wms.location.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: ListBinsQuery
 * <p>
 * Query object for listing bins under a rack.
 */
@Getter
@Builder
@AllArgsConstructor
public final class ListBinsQuery {
    private final TenantId tenantId;
    private final LocationId rackId;

    /**
     * Static factory method with validation.
     */
    public static ListBinsQuery of(TenantId tenantId, LocationId rackId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (rackId == null) {
            throw new IllegalArgumentException("RackId is required");
        }
        return ListBinsQuery.builder().tenantId(tenantId).rackId(rackId).build();
    }
}
