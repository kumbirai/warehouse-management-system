package com.ccbsa.wms.location.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.location.application.service.port.repository.LocationRepository;
import com.ccbsa.wms.location.application.service.query.dto.GetAvailableLocationsQuery;
import com.ccbsa.wms.location.application.service.query.dto.LocationQueryResult;
import com.ccbsa.wms.location.domain.core.entity.Location;

/**
 * Query Handler: GetAvailableLocationsQueryHandler
 * <p>
 * Handles retrieval of available locations.
 * <p>
 * Responsibilities:
 * - Load available locations from repository
 * - Map aggregates to query result DTOs
 * - Return optimized read model
 */
@Component
public class GetAvailableLocationsQueryHandler {
    private final LocationRepository repository;

    public GetAvailableLocationsQueryHandler(LocationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<LocationQueryResult> handle(GetAvailableLocationsQuery query) {
        // 1. Load aggregates
        List<Location> locations = repository.findAvailableLocations(query.getTenantId());

        // 2. Map to query results
        return locations.stream().map(this::toQueryResult).collect(Collectors.toList());
    }

    private LocationQueryResult toQueryResult(Location location) {
        return LocationQueryResult.builder().locationId(location.getId()).barcode(location.getBarcode()).coordinates(location.getCoordinates()).status(location.getStatus())
                .capacity(location.getCapacity()).code(location.getCode() != null ? location.getCode().getValue() : null)
                .name(location.getName() != null ? location.getName().getValue() : null).type(location.getType() != null ? location.getType().getValue() : null)
                .description(location.getDescription() != null ? location.getDescription().getValue() : null).createdAt(location.getCreatedAt())
                .lastModifiedAt(location.getLastModifiedAt()).build();
    }
}

