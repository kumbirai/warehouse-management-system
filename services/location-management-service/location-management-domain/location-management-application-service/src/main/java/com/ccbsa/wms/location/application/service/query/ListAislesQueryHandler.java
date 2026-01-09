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
import com.ccbsa.wms.location.application.service.query.dto.ListAislesQuery;
import com.ccbsa.wms.location.application.service.query.dto.LocationHierarchyItemResult;
import com.ccbsa.wms.location.application.service.query.dto.LocationHierarchyQueryResult;
import com.ccbsa.wms.location.application.service.query.dto.LocationQueryResult;
import com.ccbsa.wms.location.domain.core.exception.LocationNotFoundException;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.RequiredArgsConstructor;

import static com.ccbsa.wms.location.application.service.query.LocationTypeConstants.AISLE;
import static com.ccbsa.wms.location.application.service.query.LocationTypeConstants.RACK;

/**
 * Query Handler: ListAislesQueryHandler
 * <p>
 * Handles listing of aisles under a zone.
 */
@Component
@RequiredArgsConstructor
public class ListAislesQueryHandler {
    private final LocationViewRepository viewRepository;
    private final GetLocationQueryHandler getLocationQueryHandler;

    @Transactional(readOnly = true)
    public LocationHierarchyQueryResult handle(ListAislesQuery query) {
        // Validate query
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (query.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (query.getZoneId() == null) {
            throw new IllegalArgumentException("ZoneId is required");
        }

        // 1. Get parent zone
        LocationView zone = viewRepository.findByTenantIdAndId(query.getTenantId(), query.getZoneId())
                .orElseThrow(() -> new LocationNotFoundException(String.format("Zone not found: %s", query.getZoneId().getValueAsString())));

        LocationQueryResult parentResult = getLocationQueryHandler.handle(new GetLocationQuery(zone.getLocationId(), query.getTenantId()));

        // 2. Query aisles under zone
        List<LocationView> aisles = viewRepository.findAislesByZoneId(query.getTenantId(), query.getZoneId());

        // 3. Query all racks to calculate child counts
        List<LocationView> allRacks = viewRepository.findByTenantIdAndType(query.getTenantId(), RACK);

        // 4. Group racks by parent location ID
        Map<LocationId, List<LocationView>> racksByParent =
                allRacks.stream().filter(rack -> rack.getParentLocationId() != null).collect(Collectors.groupingBy(LocationView::getParentLocationId));

        // 5. Map aisles to hierarchy items
        List<LocationHierarchyItemResult> items = aisles.stream().map(aisle -> {
            LocationQueryResult locationResult = toLocationQueryResult(aisle, query.getTenantId());
            List<LocationView> childRacks = racksByParent.getOrDefault(aisle.getLocationId(), List.of());
            int childCount = childRacks.size();
            Map<String, Integer> statusSummary = calculateStatusSummary(childRacks);
            return LocationHierarchyItemResult.builder().location(locationResult).childCount(childCount).statusSummary(statusSummary).build();
        }).collect(Collectors.toList());

        // 6. Build result
        return LocationHierarchyQueryResult.builder().parent(parentResult).items(items).hierarchyLevel(AISLE).build();
    }

    private LocationQueryResult toLocationQueryResult(LocationView view, TenantId tenantId) {
        return getLocationQueryHandler.handle(new GetLocationQuery(view.getLocationId(), tenantId));
    }

    private Map<String, Integer> calculateStatusSummary(List<LocationView> locations) {
        return locations.stream().collect(Collectors.groupingBy(location -> location.getStatus().name(), Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
    }
}
