package com.ccbsa.wms.stock.application.dto.command;

import java.time.LocalDateTime;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command DTO: ValidateConsignmentCommandDTO
 * <p>
 * API request DTO for validating consignment data.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Defensive copy made in mapper when converting to domain command.")
public class ValidateConsignmentCommandDTO {
    @NotBlank(message = "Consignment reference is required")
    private String consignmentReference;

    @NotBlank(message = "Warehouse ID is required")
    private String warehouseId;

    @NotNull(message = "Received date is required")
    private LocalDateTime receivedAt;

    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<CreateConsignmentCommandDTO.ConsignmentLineItemDTO> lineItems;
}

