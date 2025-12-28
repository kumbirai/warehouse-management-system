package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
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

