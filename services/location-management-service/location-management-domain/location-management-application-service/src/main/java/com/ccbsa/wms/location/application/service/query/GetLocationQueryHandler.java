package com.ccbsa.wms.location.application.service.query;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.application.service.port.data.LocationViewRepository;
import com.ccbsa.wms.location.application.service.port.data.dto.LocationView;
import com.ccbsa.wms.location.application.service.query.dto.GetLocationQuery;
import com.ccbsa.wms.location.application.service.query.dto.LocationQueryResult;
import com.ccbsa.wms.location.domain.core.exception.LocationNotFoundException;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: GetLocationQueryHandler
 * <p>
 * Handles retrieval of Location read model by ID.
 * <p>
 * Responsibilities:
 * - Load Location view from data port (read model)
 * - Generate hierarchical path by traversing parent locations
 * - Map view to query result DTO
 * - Return optimized read model
 * <p>
 * Uses data port (LocationViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GetLocationQueryHandler {
    private final LocationViewRepository viewRepository;

    @Transactional(readOnly = true)
    public LocationQueryResult handle(GetLocationQuery query) {
        // 1. Load read model (view) from data port
        var locationView = viewRepository.findByTenantIdAndId(query.getTenantId(), query.getLocationId())
                .orElseThrow(() -> new LocationNotFoundException(String.format("Location not found: %s", query.getLocationId().getValueAsString())));

        // 2. Generate hierarchical path
        String path = generatePath(locationView, query.getTenantId());

        // 3. Map view to query result
        return LocationQueryResult.builder().locationId(locationView.getLocationId()).barcode(locationView.getBarcode()).coordinates(locationView.getCoordinates())
                .status(locationView.getStatus()).capacity(locationView.getCapacity()).code(locationView.getCode()).name(locationView.getName()).type(locationView.getType())
                .path(path).description(locationView.getDescription()).parentLocationId(locationView.getParentLocationId())
                .createdAt(locationView.getCreatedAt()).lastModifiedAt(locationView.getLastModifiedAt()).build();
    }

    /**
     * Generates a hierarchical path for the location by recursively traversing up the parent hierarchy.
     * For warehouses, returns "/{code}".
     * For child locations, returns "/{warehouseCode}/{zoneCode}/{aisleCode}/{rackCode}/{binCode}" by recursively loading parents.
     *
     * @param locationView Location view (with stored parentLocationId)
     * @param tenantId     Tenant identifier
     * @return Path string with full hierarchy
     */
    private String generatePath(LocationView locationView, TenantId tenantId) {
        String locationCode = locationView.getCode();
        if (locationCode == null || locationCode.trim().isEmpty()) {
            locationCode = locationView.getBarcode().getValue();
        }

        // If this is a warehouse (no parent), return "/{code}"
        if (locationView.getParentLocationId() == null) {
            return String.format("/%s", locationCode);
        }

        // For child locations, recursively build path by traversing up the hierarchy using stored parentLocationId
        Set<LocationId> visitedIds = new HashSet<>();
        String parentPath = buildParentPathRecursively(locationView.getParentLocationId(), tenantId, visitedIds);
        // Build hierarchical path: /{parentPath}/{childCode}
        String hierarchicalPath = String.format("%s/%s", parentPath, locationCode);
        log.debug("Generated hierarchical path: {} for location: {}", hierarchicalPath, locationView.getLocationId().getValueAsString());
        return hierarchicalPath;
    }

    /**
     * Recursively builds the parent path by traversing up the location hierarchy using stored parent_location_id.
     * This method traverses from the given parent location ID up to the warehouse (root of hierarchy).
     *
     * @param parentLocationId Parent location ID to start traversal from
     * @param tenantId         Tenant identifier
     * @param visitedIds       Set of visited location IDs to prevent infinite loops
     * @return Path string (e.g., "/WH-12/ZONE-X" for a zone)
     */
    private String buildParentPathRecursively(LocationId parentLocationId, TenantId tenantId, Set<LocationId> visitedIds) {
        // Prevent infinite loops
        if (visitedIds.contains(parentLocationId)) {
            log.warn("Circular reference detected in location hierarchy: {}", parentLocationId.getValueAsString());
            return "";
        }
        visitedIds.add(parentLocationId);

        var parentLocationView = viewRepository.findByTenantIdAndId(tenantId, parentLocationId)
                .orElseThrow(() -> new LocationNotFoundException(String.format("Parent location not found during path generation: %s", parentLocationId.getValueAsString())));

        String locationCode = parentLocationView.getCode();
        if (locationCode == null || locationCode.trim().isEmpty()) {
            locationCode = parentLocationView.getBarcode().getValue();
        }

        // If this is a warehouse (type is WAREHOUSE or no parent), return "/{code}"
        String locationType = parentLocationView.getType();
        if ((locationType != null && "WAREHOUSE".equalsIgnoreCase(locationType.trim())) || parentLocationView.getParentLocationId() == null) {
            return String.format("/%s", locationCode);
        }

        // Recursively build path for parent's parent
        String parentPath = buildParentPathRecursively(parentLocationView.getParentLocationId(), tenantId, visitedIds);
        return String.format("%s/%s", parentPath, locationCode);
    }
}

