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
public class TenantResponse {
    private String tenantId;
    private String name;
    private String email;
    private String status;
    private TenantConfiguration configuration;
    private String createdAt;
    private String updatedAt;
}

