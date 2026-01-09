package com.ccbsa.wms.gateway.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantRequest {
    private String tenantId;
    private String name;
    @JsonProperty("emailAddress")
    private String emailAddress;
    private String phone;
    private String address;
    private String keycloakRealmName;
    private Boolean usePerTenantRealm;
}

