package com.ccbsa.wms.gateway.api.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnblockLocationResultDTO {
    private UUID locationId;
    private String status; // LocationStatus enum value
}

