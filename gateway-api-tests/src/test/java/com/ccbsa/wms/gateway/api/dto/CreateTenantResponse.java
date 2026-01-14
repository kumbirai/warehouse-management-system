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
public class CreateTenantResponse {
    private String tenantId;
    private boolean success;
    private String message;
}

