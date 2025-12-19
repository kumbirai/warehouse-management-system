package com.ccbsa.wms.gateway.api.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConsignmentRequest {
    private String productId;
    private String locationId;
    private Integer quantity;
    private String batchNumber;
    private LocalDate expirationDate;
    private LocalDate manufactureDate;
    private String supplierReference;
    private LocalDate receivedDate;
}

