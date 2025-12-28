package com.ccbsa.wms.location.application.dto.query;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query Result DTO: ListLocationsQueryResultDTO
 * <p>
 * API response DTO for list locations query results.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Lists are immutable when returned from API.")
public final class ListLocationsQueryResultDTO {
    private List<LocationQueryResultDTO> locations;
    private Integer totalCount;
    private Integer page;
    private Integer size;
}

