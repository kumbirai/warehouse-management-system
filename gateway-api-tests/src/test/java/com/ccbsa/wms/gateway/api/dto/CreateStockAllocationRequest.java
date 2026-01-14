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
public class CreateStockAllocationRequest {
    private UUID productId;
    private UUID locationId; // Optional - null for FEFO allocation
    private Integer quantity;
    private String allocationType; // PICKING_ORDER, RESERVATION, OTHER
    private String referenceId; // Required for PICKING_ORDER
    private String notes;
}

