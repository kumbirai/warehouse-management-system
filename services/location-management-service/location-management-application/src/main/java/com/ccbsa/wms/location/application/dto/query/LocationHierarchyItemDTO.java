package com.ccbsa.wms.location.application.dto.query;

import java.util.Map;

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
public class LocationHierarchyItemDTO {
    private LocationQueryResultDTO location;
    private Integer childCount;
    private Map<String, Integer> statusSummary;
}
