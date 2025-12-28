package com.ccbsa.wms.location.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: ListLocationsQuery
 * <p>
 * Query object for listing locations with optional filters.
 */
@Getter
@Builder
@AllArgsConstructor
public final class ListLocationsQuery {
    private final TenantId tenantId;
    private final Integer page;
    private final Integer size;
    private final String zone;
    private final String status;
    private final String search;

    /**
     * Static factory method with validation.
     */
    public static ListLocationsQuery of(TenantId tenantId, Integer page, Integer size, String zone, String status, String search) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        return ListLocationsQuery.builder().tenantId(tenantId).page(page).size(size).zone(zone).status(status).search(search).build();
    }
}

