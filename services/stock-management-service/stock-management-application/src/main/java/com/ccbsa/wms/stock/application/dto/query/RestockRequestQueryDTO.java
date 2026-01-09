package com.ccbsa.wms.stock.application.dto.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query DTO: RestockRequestQueryDTO
 * <p>
 * API response DTO for restock request queries.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "DTOs are data transfer objects - fields are meant to be exposed")
public class RestockRequestQueryDTO {
    private String restockRequestId;
    private String productId;
    private String locationId;
    private BigDecimal currentQuantity;
    private BigDecimal minimumQuantity;
    private BigDecimal maximumQuantity;
    private BigDecimal requestedQuantity;
    private String priority;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime sentToD365At;
    private String d365OrderReference;
}
