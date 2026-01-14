package com.ccbsa.wms.gateway.api.dto;

import java.util.List;
import java.util.Map;

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
public class TenantConfiguration {
    private Integer maxUsers;
    private Integer storageQuotaMB;
    private List<String> features;
    private Map<String, String> settings;
}

