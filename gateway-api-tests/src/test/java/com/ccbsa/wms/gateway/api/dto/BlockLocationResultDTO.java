package com.ccbsa.wms.gateway.api.dto;

import java.util.UUID;

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
public class BlockLocationResultDTO {
    private UUID locationId;
    private String status; // LocationStatus enum value
}

