package com.ccbsa.wms.location.application.service.query.dto;

import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: LocationHierarchyQueryResult
 * <p>
 * Result object returned from hierarchical location queries. Contains parent location (if any) and list of child locations.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Defensive copy made in constructor and getter returns immutable view.")
public final class LocationHierarchyQueryResult {
    private final LocationQueryResult parent;
    private final List<LocationHierarchyItemResult> items;
    private final String hierarchyLevel;

    public LocationHierarchyQueryResult(LocationQueryResult parent, List<LocationHierarchyItemResult> items, String hierarchyLevel) {
        this.parent = parent;
        // Defensive copy to prevent external modification
        this.items = items != null ? List.copyOf(items) : List.of();
        if (hierarchyLevel == null || hierarchyLevel.trim().isEmpty()) {
            throw new IllegalArgumentException("HierarchyLevel is required");
        }
        this.hierarchyLevel = hierarchyLevel.trim().toUpperCase();
    }

    /**
     * Returns an unmodifiable view of the items list.
     *
     * @return Unmodifiable list of hierarchy items
     */
    public List<LocationHierarchyItemResult> getItems() {
        return Collections.unmodifiableList(items);
    }
}
