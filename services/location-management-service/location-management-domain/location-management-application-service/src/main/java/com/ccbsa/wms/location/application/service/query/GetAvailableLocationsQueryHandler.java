package com.ccbsa.wms.location.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.location.application.service.port.data.LocationViewRepository;
import com.ccbsa.wms.location.application.service.query.dto.GetAvailableLocationsQuery;
import com.ccbsa.wms.location.application.service.query.dto.LocationQueryResult;

import lombok.RequiredArgsConstructor;

/**
 * Query Handler: GetAvailableLocationsQueryHandler
 * <p>
 * Handles retrieval of available location read models.
 * <p>
 * Responsibilities:
 * - Load available location views from data port (read model)
 * - Map views to query result DTOs
 * - Return optimized read model
 * <p>
 * Uses data port (LocationViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@RequiredArgsConstructor
public class GetAvailableLocationsQueryHandler {
    private final LocationViewRepository viewRepository;

    @Transactional(readOnly = true)
    public List<LocationQueryResult> handle(GetAvailableLocationsQuery query) {
        // 1. Load read models (views) from data port
        List<com.ccbsa.wms.location.application.service.port.data.dto.LocationView> locationViews = viewRepository.findAvailableLocations(query.getTenantId());

        // 2. Map views to query results
        return locationViews.stream().map(this::toQueryResult).collect(Collectors.toList());
    }

    private LocationQueryResult toQueryResult(com.ccbsa.wms.location.application.service.port.data.dto.LocationView view) {
        return LocationQueryResult.builder().locationId(view.getLocationId()).barcode(view.getBarcode()).coordinates(view.getCoordinates()).status(view.getStatus())
                .capacity(view.getCapacity()).code(view.getCode()).name(view.getName()).type(view.getType()).description(view.getDescription()).createdAt(view.getCreatedAt())
                .lastModifiedAt(view.getLastModifiedAt()).build();
    }
}

