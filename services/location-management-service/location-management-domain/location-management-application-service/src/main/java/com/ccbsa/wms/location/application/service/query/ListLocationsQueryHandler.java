package com.ccbsa.wms.location.application.service.query;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.location.application.service.port.repository.LocationRepository;
import com.ccbsa.wms.location.application.service.query.dto.ListLocationsQuery;
import com.ccbsa.wms.location.application.service.query.dto.ListLocationsQueryResult;
import com.ccbsa.wms.location.application.service.query.dto.LocationQueryResult;
import com.ccbsa.wms.location.domain.core.entity.Location;

/**
 * Query Handler: ListLocationsQueryHandler
 * <p>
 * Handles listing of Location aggregates with optional filtering.
 * <p>
 * Responsibilities:
 * - Load Location aggregates from repository
 * - Apply filters (zone, status, search)
 * - Map aggregates to query result DTOs
 * - Return paginated results
 */
@Component
public class ListLocationsQueryHandler {

    private final LocationRepository repository;

    public ListLocationsQueryHandler(LocationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ListLocationsQueryResult handle(ListLocationsQuery query) {
        // 1. Load all locations for tenant
        List<Location> allLocations = repository.findByTenantId(query.getTenantId());

        // 2. Apply filters
        List<Location> filteredLocations =
                allLocations.stream().filter(location -> matchesZone(location, query.getZone())).filter(location -> matchesStatus(location, query.getStatus()))
                        .filter(location -> matchesSearch(location, query.getSearch())).collect(Collectors.toList());

        // 3. Apply pagination
        int page = query.getPage() != null ? query.getPage() : 0;
        int size = query.getSize() != null ? query.getSize() : 100;
        int totalCount = filteredLocations.size();
        int start = page * size;

        // Handle edge case: if start is beyond the list size, return empty list
        List<Location> paginatedLocations;
        if (start >= totalCount) {
            paginatedLocations = List.of();
        } else {
            int end = Math.min(start + size, totalCount);
            paginatedLocations = filteredLocations.subList(start, end);
        }

        // 4. Map to query results
        List<LocationQueryResult> locationResults = paginatedLocations.stream().map(this::toLocationQueryResult).collect(Collectors.toList());

        // 5. Build result
        return ListLocationsQueryResult.builder().locations(locationResults).totalCount(totalCount).page(page).size(size).build();
    }

    private boolean matchesZone(Location location, String zone) {
        if (zone == null || zone.isBlank()) {
            return true;
        }
        return location.getCoordinates() != null && location.getCoordinates().getZone() != null && location.getCoordinates().getZone().equalsIgnoreCase(zone);
    }

    private boolean matchesStatus(Location location, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        return location.getStatus() != null && location.getStatus().name().equalsIgnoreCase(status);
    }

    private boolean matchesSearch(Location location, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String searchLower = search.toLowerCase(Locale.ROOT);
        return (location.getBarcode() != null && location.getBarcode().getValue().toLowerCase(Locale.ROOT).contains(searchLower)) || (location.getCoordinates() != null
                && location.getCoordinates().getZone() != null && location.getCoordinates().getZone().toLowerCase(Locale.ROOT).contains(searchLower)) || (
                location.getCoordinates() != null && location.getCoordinates().getAisle() != null && location.getCoordinates().getAisle().toLowerCase(Locale.ROOT)
                        .contains(searchLower)) || (location.getDescription() != null && location.getDescription().getValue() != null && location.getDescription().getValue()
                .toLowerCase(Locale.ROOT).contains(searchLower));
    }

    private LocationQueryResult toLocationQueryResult(Location location) {
        return LocationQueryResult.builder().locationId(location.getId()).barcode(location.getBarcode()).coordinates(location.getCoordinates()).status(location.getStatus())
                .capacity(location.getCapacity()).code(location.getCode() != null ? location.getCode().getValue() : null)
                .name(location.getName() != null ? location.getName().getValue() : null).type(location.getType() != null ? location.getType().getValue() : null)
                .description(location.getDescription() != null ? location.getDescription().getValue() : null).createdAt(location.getCreatedAt())
                .lastModifiedAt(location.getLastModifiedAt()).build();
    }
}

