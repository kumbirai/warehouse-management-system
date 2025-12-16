package com.ccbsa.wms.location.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.location.application.service.port.repository.LocationRepository;
import com.ccbsa.wms.location.application.service.query.dto.GetLocationQuery;
import com.ccbsa.wms.location.application.service.query.dto.LocationQueryResult;
import com.ccbsa.wms.location.domain.core.exception.LocationNotFoundException;

/**
 * Query Handler: GetLocationQueryHandler
 * <p>
 * Handles retrieval of Location aggregate by ID.
 * <p>
 * Responsibilities:
 * - Load Location aggregate from repository
 * - Map aggregate to query result DTO
 * - Return optimized read model
 */
@Component
public class GetLocationQueryHandler {

    private final LocationRepository repository;

    public GetLocationQueryHandler(LocationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public LocationQueryResult handle(GetLocationQuery query) {
        // 1. Load aggregate
        com.ccbsa.wms.location.domain.core.entity.Location location = repository
                .findByIdAndTenantId(query.getLocationId(), query.getTenantId())
                .orElseThrow(() -> new LocationNotFoundException(
                        String.format("Location not found: %s", query.getLocationId().getValueAsString())
                ));

        // 2. Map to query result
        return LocationQueryResult.builder()
                .locationId(location.getId())
                .barcode(location.getBarcode())
                .coordinates(location.getCoordinates())
                .status(location.getStatus())
                .capacity(location.getCapacity())
                .description(location.getDescription())
                .createdAt(location.getCreatedAt())
                .lastModifiedAt(location.getLastModifiedAt())
                .build();
    }
}

