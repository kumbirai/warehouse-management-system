package com.ccbsa.wms.location.application.dto.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query Result DTO: LocationHierarchyItemDTO
 * <p>
 * Represents a location item in a hierarchy query result with metadata about its children.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "DTOs are immutable after construction. Defensive copies created in callers and getters return defensive copies")
public class LocationHierarchyItemDTO {
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "LocationQueryResultDTO is a DTO and is immutable after construction")
    private LocationQueryResultDTO location;
    private Integer childCount;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder")
    private Map<String, Integer> statusSummary;

    /**
     * Returns a defensive copy of the status summary map to prevent external modification.
     *
     * @return unmodifiable copy of the status summary map
     */
    public Map<String, Integer> getStatusSummary() {
        if (statusSummary == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<>(statusSummary));
    }
}
