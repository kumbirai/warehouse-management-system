package com.ccbsa.wms.stock.application.dto.command;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command Result DTO: ValidateConsignmentResultDTO
 * <p>
 * API response DTO for consignment validation.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Lists are immutable when returned from API.")
public class ValidateConsignmentResultDTO {
    private boolean valid;
    private List<String> validationErrors;
}

