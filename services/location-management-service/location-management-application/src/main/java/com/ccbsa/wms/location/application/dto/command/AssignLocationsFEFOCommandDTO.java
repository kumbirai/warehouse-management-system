package com.ccbsa.wms.location.application.dto.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command DTO: AssignLocationsFEFOCommandDTO
 * <p>
 * API request DTO for FEFO location assignment.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Defensive copy made in mapper when converting to domain command.")
public class AssignLocationsFEFOCommandDTO {
    @NotEmpty(message = "At least one stock item is required")
    @Valid
    private List<StockItemAssignmentRequestDTO> stockItems;

    /**
     * Nested DTO for stock item assignment request.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockItemAssignmentRequestDTO {
        private String stockItemId;
        private BigDecimal quantity;
        private LocalDate expirationDate;
        private String classification;
    }
}

