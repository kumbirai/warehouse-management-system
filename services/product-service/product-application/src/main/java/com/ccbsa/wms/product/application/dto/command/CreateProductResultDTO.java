package com.ccbsa.wms.product.application.dto.command;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result DTO: CreateProductResultDTO
 * <p>
 * API response DTO for product creation result.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class CreateProductResultDTO {
    private String productId;
    private String productCode;
    private String description;
    private String primaryBarcode;
    private LocalDateTime createdAt;
}

