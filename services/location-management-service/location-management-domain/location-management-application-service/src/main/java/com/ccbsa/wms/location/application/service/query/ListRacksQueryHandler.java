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
import com.ccbsa.wms.location.application.service.query.dto.ListRacksQuery;
import com.ccbsa.wms.location.application.service.query.dto.LocationHierarchyItemResult;
import com.ccbsa.wms.location.application.service.query.dto.LocationHierarchyQueryResult;
import com.ccbsa.wms.location.application.service.query.dto.LocationQueryResult;
import com.ccbsa.wms.location.domain.core.exception.LocationNotFoundException;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.RequiredArgsConstructor;

import static com.ccbsa.wms.location.application.service.query.LocationTypeConstants.BIN;
import static com.ccbsa.wms.location.application.service.query.LocationTypeConstants.RACK;

/**
 * Query Handler: ListRacksQueryHandler
 * <p>
 * Handles listing of racks under an aisle.
 */
@Component
@RequiredArgsConstructor
public class ListRacksQueryHandler {
    private final LocationViewRepository viewRepository;
    private final GetLocationQueryHandler getLocationQueryHandler;

    @Transactional(readOnly = true)
    public LocationHierarchyQueryResult handle(ListRacksQuery query) {
        // Validate query
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (query.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (query.getAisleId() == null) {
            throw new IllegalArgumentException("AisleId is required");
        }

        // 1. Get parent aisle
        LocationView aisle = viewRepository.findByTenantIdAndId(query.getTenantId(), query.getAisleId())
                .orElseThrow(() -> new LocationNotFoundException(String.format("Aisle not found: %s", query.getAisleId().getValueAsString())));

        LocationQueryResult parentResult = getLocationQueryHandler.handle(new GetLocationQuery(aisle.getLocationId(), query.getTenantId()));

        // 2. Query racks under aisle
        List<LocationView> racks = viewRepository.findRacksByAisleId(query.getTenantId(), query.getAisleId());

        // 3. Query all bins to calculate child counts
        List<LocationView> allBins = viewRepository.findByTenantIdAndType(query.getTenantId(), BIN);

        // 4. Group bins by parent location ID
        Map<LocationId, List<LocationView>> binsByParent =
                allBins.stream().filter(bin -> bin.getParentLocationId() != null).collect(Collectors.groupingBy(LocationView::getParentLocationId));

        // 5. Map racks to hierarchy items
        List<LocationHierarchyItemResult> items = racks.stream().map(rack -> {
            LocationQueryResult locationResult = toLocationQueryResult(rack, query.getTenantId());
            List<LocationView> childBins = binsByParent.getOrDefault(rack.getLocationId(), List.of());
            int childCount = childBins.size();
            Map<String, Integer> statusSummary = calculateStatusSummary(childBins);
            return LocationHierarchyItemResult.builder().location(locationResult).childCount(childCount).statusSummary(statusSummary).build();
        }).collect(Collectors.toList());

        // 6. Build result
        return LocationHierarchyQueryResult.builder().parent(parentResult).items(items).hierarchyLevel(RACK).build();
    }

    private LocationQueryResult toLocationQueryResult(LocationView view, TenantId tenantId) {
        return getLocationQueryHandler.handle(new GetLocationQuery(view.getLocationId(), tenantId));
    }

    private Map<String, Integer> calculateStatusSummary(List<LocationView> locations) {
        return locations.stream().collect(Collectors.groupingBy(location -> location.getStatus().name(), Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
    }
}
