package com.ccbsa.wms.location.application.service.query;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.location.application.service.port.repository.LocationRepository;
import com.ccbsa.wms.location.application.service.query.dto.GetLocationQuery;
import com.ccbsa.wms.location.application.service.query.dto.LocationQueryResult;
import com.ccbsa.wms.location.domain.core.entity.Location;
import com.ccbsa.wms.location.domain.core.exception.LocationNotFoundException;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

/**
 * Query Handler: GetLocationQueryHandler
 * <p>
 * Handles retrieval of Location aggregate by ID.
 * <p>
 * Responsibilities: - Load Location aggregate from repository - Map aggregate to query result DTO - Return optimized read model
 */
@Component
public class GetLocationQueryHandler {
    private static final Logger logger = LoggerFactory.getLogger(GetLocationQueryHandler.class);

    private final LocationRepository repository;

    public GetLocationQueryHandler(LocationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public LocationQueryResult handle(GetLocationQuery query) {
        // 1. Load aggregate
        Location location = repository.findByIdAndTenantId(query.getLocationId(), query.getTenantId())
                .orElseThrow(() -> new LocationNotFoundException(String.format("Location not found: %s", query.getLocationId().getValueAsString())));

        // 2. Map to query result
        return LocationQueryResult.builder().locationId(location.getId()).barcode(location.getBarcode()).coordinates(location.getCoordinates()).status(location.getStatus())
                .capacity(location.getCapacity()).code(location.getCode() != null ? location.getCode().getValue() : null)
                .name(location.getName() != null ? location.getName().getValue() : null).type(location.getType() != null ? location.getType().getValue() : null)
                .path(generatePath(location)).description(location.getDescription() != null ? location.getDescription().getValue() : null).createdAt(location.getCreatedAt())
                .lastModifiedAt(location.getLastModifiedAt()).build();
    }

    /**
     * Generates a hierarchical path for the location by recursively traversing up the parent hierarchy.
     * For warehouses, returns "/{code}".
     * For child locations, returns "/{warehouseCode}/{zoneCode}/{aisleCode}/{rackCode}/{binCode}" by recursively loading parents.
     *
     * @param location Location aggregate (with stored parentLocationId)
     * @return Path string with full hierarchy
     */
    private String generatePath(Location location) {
        String locationCode = location.getCode() != null ? location.getCode().getValue() : null;
        if (locationCode == null || locationCode.trim().isEmpty()) {
            locationCode = location.getBarcode().getValue();
        }

        // If this is a warehouse (no parent), return "/{code}"
        if (location.getParentLocationId() == null) {
            return String.format("/%s", locationCode);
        }

        // For child locations, recursively build path by traversing up the hierarchy using stored parentLocationId
        Set<LocationId> visitedIds = new HashSet<>();
        String parentPath = buildParentPathRecursively(location.getParentLocationId(), location.getTenantId(), visitedIds);
        // Build hierarchical path: /{parentPath}/{childCode}
        String hierarchicalPath = String.format("%s/%s", parentPath, locationCode);
        logger.debug("Generated hierarchical path: {} for location: {}", hierarchicalPath, location.getId().getValueAsString());
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
    private String buildParentPathRecursively(LocationId parentLocationId, com.ccbsa.common.domain.valueobject.TenantId tenantId, Set<LocationId> visitedIds) {
        // Prevent infinite loops
        if (visitedIds.contains(parentLocationId)) {
            logger.warn("Circular reference detected in location hierarchy: {}", parentLocationId.getValueAsString());
            return "";
        }
        visitedIds.add(parentLocationId);

        Location parentLocation = repository.findByIdAndTenantId(parentLocationId, tenantId)
                .orElseThrow(() -> new LocationNotFoundException(String.format("Parent location not found during path generation: %s", parentLocationId.getValueAsString())));

        String locationCode = parentLocation.getCode() != null ? parentLocation.getCode().getValue() : null;
        if (locationCode == null || locationCode.trim().isEmpty()) {
            locationCode = parentLocation.getBarcode().getValue();
        }

        // If this is a warehouse (type is WAREHOUSE or no parent), return "/{code}"
        String locationType = parentLocation.getType() != null ? parentLocation.getType().getValue() : null;
        if ((locationType != null && "WAREHOUSE".equalsIgnoreCase(locationType.trim())) || parentLocation.getParentLocationId() == null) {
            return String.format("/%s", locationCode);
        }

        // Recursively build path for parent's parent
        String parentPath = buildParentPathRecursively(parentLocation.getParentLocationId(), tenantId, visitedIds);
        return String.format("%s/%s", parentPath, locationCode);
    }
}

