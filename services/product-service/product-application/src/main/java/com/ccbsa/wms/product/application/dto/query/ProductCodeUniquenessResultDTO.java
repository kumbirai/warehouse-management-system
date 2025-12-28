package com.ccbsa.wms.product.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query Result DTO: ProductCodeUniquenessResultDTO
 * <p>
 * API response DTO for product code uniqueness check.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class ProductCodeUniquenessResultDTO {
    private String productCode;
    private boolean isUnique;
}

