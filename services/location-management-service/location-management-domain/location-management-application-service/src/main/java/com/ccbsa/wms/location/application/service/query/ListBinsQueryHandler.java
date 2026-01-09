package com.ccbsa.wms.location.application.service.query;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.application.service.port.data.LocationViewRepository;
import com.ccbsa.wms.location.application.service.port.data.dto.LocationView;
import com.ccbsa.wms.location.application.service.query.dto.GetLocationQuery;
import com.ccbsa.wms.location.application.service.query.dto.ListBinsQuery;
import com.ccbsa.wms.location.application.service.query.dto.LocationHierarchyItemResult;
import com.ccbsa.wms.location.application.service.query.dto.LocationHierarchyQueryResult;
import com.ccbsa.wms.location.application.service.query.dto.LocationQueryResult;
import com.ccbsa.wms.location.domain.core.exception.LocationNotFoundException;

import lombok.RequiredArgsConstructor;

import static com.ccbsa.wms.location.application.service.query.LocationTypeConstants.BIN;

/**
 * Query Handler: ListBinsQueryHandler
 * <p>
 * Handles listing of bins under a rack. Bins are leaf nodes, so they have no children.
 */
@Component
@RequiredArgsConstructor
public class ListBinsQueryHandler {
    private final LocationViewRepository viewRepository;
    private final GetLocationQueryHandler getLocationQueryHandler;

    @Transactional(readOnly = true)
    public LocationHierarchyQueryResult handle(ListBinsQuery query) {
        // Validate query
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (query.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (query.getRackId() == null) {
            throw new IllegalArgumentException("RackId is required");
        }

        // 1. Get parent rack
        LocationView rack = viewRepository.findByTenantIdAndId(query.getTenantId(), query.getRackId())
                .orElseThrow(() -> new LocationNotFoundException(String.format("Rack not found: %s", query.getRackId().getValueAsString())));

        LocationQueryResult parentResult = getLocationQueryHandler.handle(new GetLocationQuery(rack.getLocationId(), query.getTenantId()));

        // 2. Query bins under rack
        List<LocationView> bins = viewRepository.findBinsByRackId(query.getTenantId(), query.getRackId());

        // 3. Map bins to hierarchy items (bins have no children, so childCount is always 0)
        List<LocationHierarchyItemResult> items = bins.stream().map(bin -> {
            LocationQueryResult locationResult = toLocationQueryResult(bin, query.getTenantId());
            // Bins are leaf nodes - no children
            return LocationHierarchyItemResult.builder().location(locationResult).childCount(0).statusSummary(Map.of(locationResult.getStatus().name(), 1)).build();
        }).collect(Collectors.toList());

        // 4. Build result
        return LocationHierarchyQueryResult.builder().parent(parentResult).items(items).hierarchyLevel(BIN).build();
    }

    private LocationQueryResult toLocationQueryResult(LocationView view, TenantId tenantId) {
        return getLocationQueryHandler.handle(new GetLocationQuery(view.getLocationId(), tenantId));
    }
}
