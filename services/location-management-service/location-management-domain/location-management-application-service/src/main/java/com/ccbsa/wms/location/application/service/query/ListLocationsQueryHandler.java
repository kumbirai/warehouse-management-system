package com.ccbsa.wms.location.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.location.application.service.port.data.LocationViewRepository;
import com.ccbsa.wms.location.application.service.query.dto.ListLocationsQuery;
import com.ccbsa.wms.location.application.service.query.dto.ListLocationsQueryResult;
import com.ccbsa.wms.location.application.service.query.dto.LocationQueryResult;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;

/**
 * Query Handler: ListLocationsQueryHandler
 * <p>
 * Handles listing of Location read models with optional filtering.
 * <p>
 * Responsibilities:
 * - Load Location views from data port (read model)
 * - Apply filters (zone, status, search) - now done at database level
 * - Map views to query result DTOs
 * - Return paginated results
 * <p>
 * Uses data port (LocationViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@RequiredArgsConstructor
public class ListLocationsQueryHandler {
    private final LocationViewRepository viewRepository;

    @Transactional(readOnly = true)
    public ListLocationsQueryResult handle(ListLocationsQuery query) {
        // 1. Normalize pagination parameters
        // Convert 1-based page to 0-based for Spring Data JPA (which uses 0-based indexing)
        // If page is null or <= 0, default to 0 (first page)
        int page = query.getPage() != null && query.getPage() > 0 ? query.getPage() - 1 : 0;
        int size = query.getSize() != null ? query.getSize() : 100;

        // 2. Query location views with filters and pagination (database-level filtering)
        List<com.ccbsa.wms.location.application.service.port.data.dto.LocationView> locationViews =
                viewRepository.findByTenantIdWithFilters(query.getTenantId(), query.getZone(), query.getStatus(), query.getSearch(), page, size);

        // 3. Get total count for pagination metadata
        @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE", justification = "totalCount and locationResults are used in builder - SpotBugs false positive") long totalCount =
                viewRepository.countByTenantIdWithFilters(query.getTenantId(), query.getZone(), query.getStatus(), query.getSearch());

        // 4. Map views to query results
        List<LocationQueryResult> locationResults = locationViews.stream().map(this::toLocationQueryResult).collect(Collectors.toList());

        // 5. Build result
        // Return 1-based page in response for consistency with frontend (which uses 1-based)
        int responsePage = query.getPage() != null && query.getPage() > 0 ? query.getPage() : 1;
        return ListLocationsQueryResult.builder().locations(locationResults).totalCount((int) totalCount).page(responsePage).size(size).build();
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD", justification = "Method is called via method reference in stream (this::toLocationQueryResult) - SpotBugs false "
            + "positive")
    private LocationQueryResult toLocationQueryResult(com.ccbsa.wms.location.application.service.port.data.dto.LocationView view) {
        return LocationQueryResult.builder().locationId(view.getLocationId()).barcode(view.getBarcode()).coordinates(view.getCoordinates()).status(view.getStatus())
                .capacity(view.getCapacity()).code(view.getCode()).name(view.getName()).type(view.getType()).description(view.getDescription()).createdAt(view.getCreatedAt())
                .lastModifiedAt(view.getLastModifiedAt()).build();
    }
}

