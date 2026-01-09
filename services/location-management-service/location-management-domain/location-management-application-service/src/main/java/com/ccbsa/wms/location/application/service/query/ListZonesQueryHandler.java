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
import com.ccbsa.wms.location.application.service.query.dto.ListZonesQuery;
import com.ccbsa.wms.location.application.service.query.dto.LocationHierarchyItemResult;
import com.ccbsa.wms.location.application.service.query.dto.LocationHierarchyQueryResult;
import com.ccbsa.wms.location.application.service.query.dto.LocationQueryResult;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.RequiredArgsConstructor;

import static com.ccbsa.wms.location.application.service.query.LocationTypeConstants.AISLE;
import static com.ccbsa.wms.location.application.service.query.LocationTypeConstants.ZONE;

/**
 * Query Handler: ListZonesQueryHandler
 * <p>
 * Handles listing of zones under a warehouse.
 */
@Component
@RequiredArgsConstructor
public class ListZonesQueryHandler {
    private final LocationViewRepository viewRepository;
    private final GetLocationQueryHandler getLocationQueryHandler;

    @Transactional(readOnly = true)
    public LocationHierarchyQueryResult handle(ListZonesQuery query) {
        // Validate query
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (query.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (query.getWarehouseId() == null) {
            throw new IllegalArgumentException("WarehouseId is required");
        }

        // 1. Get parent warehouse
        LocationQueryResult parentResult = getLocationQueryHandler.handle(new GetLocationQuery(query.getWarehouseId(), query.getTenantId()));

        // 2. Query zones under warehouse
        List<LocationView> zones = viewRepository.findZonesByWarehouseId(query.getTenantId(), query.getWarehouseId());

        // 3. Query all aisles to calculate child counts
        List<LocationView> allAisles = viewRepository.findByTenantIdAndType(query.getTenantId(), AISLE);

        // 4. Group aisles by parent location ID
        Map<LocationId, List<LocationView>> aislesByParent =
                allAisles.stream().filter(aisle -> aisle.getParentLocationId() != null).collect(Collectors.groupingBy(LocationView::getParentLocationId));

        // 5. Map zones to hierarchy items
        List<LocationHierarchyItemResult> items = zones.stream().map(zone -> {
            LocationQueryResult locationResult = toLocationQueryResult(zone, query.getTenantId());
            List<LocationView> childAisles = aislesByParent.getOrDefault(zone.getLocationId(), List.of());
            int childCount = childAisles.size();
            Map<String, Integer> statusSummary = calculateStatusSummary(childAisles);
            return LocationHierarchyItemResult.builder().location(locationResult).childCount(childCount).statusSummary(statusSummary).build();
        }).collect(Collectors.toList());

        // 6. Build result
        return LocationHierarchyQueryResult.builder().parent(parentResult).items(items).hierarchyLevel(ZONE).build();
    }

    private LocationQueryResult toLocationQueryResult(LocationView view, TenantId tenantId) {
        return getLocationQueryHandler.handle(new GetLocationQuery(view.getLocationId(), tenantId));
    }

    private Map<String, Integer> calculateStatusSummary(List<LocationView> locations) {
        return locations.stream().collect(Collectors.groupingBy(location -> location.getStatus().name(), Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
    }
}
