package com.ccbsa.wms.location.application.dto.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query Result DTO: LocationHierarchyQueryResultDTO
 * <p>
 * Response DTO for hierarchical location queries. Contains parent location (if any) and list of child locations.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "DTOs are immutable after construction. Defensive copies created in callers and getters return defensive copies")
public class LocationHierarchyQueryResultDTO {
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "LocationQueryResultDTO is a DTO and is immutable after construction")
    private LocationQueryResultDTO parent;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder")
    private List<LocationHierarchyItemDTO> items;
    private String hierarchyLevel;

    /**
     * Returns a defensive copy of the items list to prevent external modification.
     *
     * @return unmodifiable copy of the items list
     */
    public List<LocationHierarchyItemDTO> getItems() {
        if (items == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(items));
    }
}
