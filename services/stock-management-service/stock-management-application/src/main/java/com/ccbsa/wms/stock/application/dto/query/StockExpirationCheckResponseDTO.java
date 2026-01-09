package com.ccbsa.wms.stock.application.dto.query;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO: StockExpirationCheckResponseDTO
 * <p>
 * API response DTO for stock expiration check.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockExpirationCheckResponseDTO {
    private boolean expired;
    private LocalDate expirationDate;
    private String classification;
    private Integer daysUntilExpiration;
    private String message;
}
