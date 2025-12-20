package com.ccbsa.wms.gateway.api.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListLocationsResponse {
    private List<LocationResponse> locations;
    private Integer totalCount;
    private Integer page;
    private Integer size;
}

