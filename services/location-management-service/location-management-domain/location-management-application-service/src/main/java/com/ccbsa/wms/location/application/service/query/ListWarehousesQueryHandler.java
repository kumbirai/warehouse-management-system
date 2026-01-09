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
import com.ccbsa.wms.location.application.service.query.dto.ListWarehousesQuery;
import com.ccbsa.wms.location.application.service.query.dto.LocationHierarchyItemResult;
import com.ccbsa.wms.location.application.service.query.dto.LocationHierarchyQueryResult;
import com.ccbsa.wms.location.application.service.query.dto.LocationQueryResult;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.RequiredArgsConstructor;

import static com.ccbsa.wms.location.application.service.query.LocationTypeConstants.WAREHOUSE;
import static com.ccbsa.wms.location.application.service.query.LocationTypeConstants.ZONE;

/**
 * Query Handler: ListWarehousesQueryHandler
 * <p>
 * Handles listing of warehouses (locations with type WAREHOUSE).
 */
@Component
@RequiredArgsConstructor
public class ListWarehousesQueryHandler {
    private final LocationViewRepository viewRepository;
    private final GetLocationQueryHandler getLocationQueryHandler;

    @Transactional(readOnly = true)
    public LocationHierarchyQueryResult handle(ListWarehousesQuery query) {
        // Validate query
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (query.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }

        // 1. Query all warehouses
        List<LocationView> warehouses = viewRepository.findWarehousesByTenantId(query.getTenantId());

        // 2. Query all zones to calculate child counts
        List<LocationView> allZones = viewRepository.findByTenantIdAndType(query.getTenantId(), ZONE);

        // 3. Group zones by parent location ID for efficient lookup
        Map<LocationId, List<LocationView>> zonesByParent =
                allZones.stream().filter(zone -> zone.getParentLocationId() != null).collect(Collectors.groupingBy(LocationView::getParentLocationId));

        // 4. Map warehouses to hierarchy items with child counts and status summaries
        List<LocationHierarchyItemResult> items = warehouses.stream().map(warehouse -> {
            LocationQueryResult locationResult = toLocationQueryResult(warehouse, query.getTenantId());
            List<LocationView> childZones = zonesByParent.getOrDefault(warehouse.getLocationId(), List.of());
            int childCount = childZones.size();
            Map<String, Integer> statusSummary = calculateStatusSummary(childZones);
            return LocationHierarchyItemResult.builder().location(locationResult).childCount(childCount).statusSummary(statusSummary).build();
        }).collect(Collectors.toList());

        // 5. Build result (no parent for warehouses)
        return LocationHierarchyQueryResult.builder().parent(null).items(items).hierarchyLevel(WAREHOUSE).build();
    }

    private LocationQueryResult toLocationQueryResult(LocationView view, TenantId tenantId) {
        // Use GetLocationQueryHandler to generate path
        return getLocationQueryHandler.handle(new GetLocationQuery(view.getLocationId(), tenantId));
    }

    private Map<String, Integer> calculateStatusSummary(List<LocationView> locations) {
        return locations.stream().collect(Collectors.groupingBy(location -> location.getStatus().name(), Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
    }
}
