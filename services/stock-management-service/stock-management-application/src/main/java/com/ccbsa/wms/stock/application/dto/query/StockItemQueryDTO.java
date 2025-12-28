package com.ccbsa.wms.stock.application.dto.query;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query DTO: StockItemQueryDTO
 * <p>
 * API response DTO for stock item queries.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockItemQueryDTO {
    private String stockItemId;
    private String productId;
    private String locationId;
    private Integer quantity;
    private LocalDate expirationDate;
    private String classification;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
}

