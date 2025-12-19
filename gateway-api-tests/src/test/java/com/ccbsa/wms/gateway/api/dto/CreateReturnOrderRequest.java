package com.ccbsa.wms.gateway.api.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReturnOrderRequest {
    private String originalOrderId;
    private String customerId;
    private List<ReturnItem> items;
    private LocalDate returnDate;
}

