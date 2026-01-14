package com.ccbsa.wms.gateway.api.dto;

import java.time.LocalDateTime;

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
public class CreateProductResponse {
    private String productId;
    private String productCode;
    private String description;
    private String primaryBarcode;
    private LocalDateTime createdAt;
}

