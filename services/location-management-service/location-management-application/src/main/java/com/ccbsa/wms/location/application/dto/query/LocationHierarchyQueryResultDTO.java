package com.ccbsa.wms.location.application.dto.query;

import java.util.List;

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
public class LocationHierarchyQueryResultDTO {
    private LocationQueryResultDTO parent;
    private List<LocationHierarchyItemDTO> items;
    private String hierarchyLevel;
}
