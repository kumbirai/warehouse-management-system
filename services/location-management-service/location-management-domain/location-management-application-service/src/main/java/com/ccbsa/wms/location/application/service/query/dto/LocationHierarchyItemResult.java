package com.ccbsa.wms.location.application.service.query.dto;

import java.util.Collections;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: LocationHierarchyItemResult
 * <p>
 * Represents a location item in a hierarchy query result with metadata about its children.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores map directly. Defensive copy made in constructor and getter returns immutable view.")
public final class LocationHierarchyItemResult {
    private final LocationQueryResult location;
    private final Integer childCount;
    private final Map<String, Integer> statusSummary;

    public LocationHierarchyItemResult(LocationQueryResult location, Integer childCount, Map<String, Integer> statusSummary) {
        if (location == null) {
            throw new IllegalArgumentException("Location is required");
        }
        this.location = location;
        this.childCount = childCount != null ? childCount : 0;
        // Defensive copy to prevent external modification
        this.statusSummary = statusSummary != null ? Map.copyOf(statusSummary) : Map.of();
    }

    /**
     * Returns an unmodifiable view of the status summary map.
     *
     * @return Unmodifiable map of status counts
     */
    public Map<String, Integer> getStatusSummary() {
        return Collections.unmodifiableMap(statusSummary);
    }
}
