package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLocationResponse {
    private String locationId;
    private String code;
    private String name;
    private String type;
    private String path;
}

