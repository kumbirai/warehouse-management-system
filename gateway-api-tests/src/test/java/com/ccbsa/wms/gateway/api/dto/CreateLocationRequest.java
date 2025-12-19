package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLocationRequest {
    private String code;
    private String name;
    private String description;
    private String type;
    private String parentLocationId;
    private Integer capacity;
    private LocationDimensions dimensions;
}

