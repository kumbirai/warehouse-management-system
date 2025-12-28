package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStockAllocationResponse {
    private UUID allocationId;
    private UUID productId;
    private UUID locationId;
    private UUID stockItemId;
    private Integer quantity;
    private String allocationType; // PICKING_ORDER, RESERVATION, OTHER
    private String referenceId;
    private String status; // PENDING, ALLOCATED, RELEASED, PICKED
    private LocalDateTime allocatedAt;
}

