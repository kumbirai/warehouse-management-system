package com.ccbsa.wms.gateway.api.dto;

import java.time.LocalDateTime;
import java.util.List;

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
public class ValidateConsignmentRequest {
    private String consignmentReference;
    private String warehouseId;
    private LocalDateTime receivedAt;
    private List<CreateConsignmentRequest.ConsignmentLineItem> lineItems;
}
