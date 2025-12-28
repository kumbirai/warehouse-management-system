package com.ccbsa.wms.product.application.dto.query;

import java.time.LocalDateTime;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query Result DTO: ProductQueryResultDTO
 * <p>
 * API response DTO for product query results.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Lists are immutable when returned from API.")
public final class ProductQueryResultDTO {
    private String productId;
    private String productCode;
    private String description;
    private String primaryBarcode;
    private List<String> secondaryBarcodes;
    private String unitOfMeasure;
    private String category;
    private String brand;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
}

