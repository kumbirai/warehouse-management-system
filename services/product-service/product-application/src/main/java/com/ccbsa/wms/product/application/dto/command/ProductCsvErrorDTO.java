package com.ccbsa.wms.product.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Error DTO: ProductCsvErrorDTO
 * <p>
 * Represents an error encountered while processing a CSV row.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class ProductCsvErrorDTO {
    private long rowNumber;
    private String productCode;
    private String errorMessage;
}

