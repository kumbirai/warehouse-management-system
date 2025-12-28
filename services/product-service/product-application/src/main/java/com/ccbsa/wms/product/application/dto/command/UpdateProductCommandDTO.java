package com.ccbsa.wms.product.application.dto.command;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command DTO: UpdateProductCommandDTO
 * <p>
 * API request DTO for updating an existing product.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Defensive copy made in mapper when converting to domain command.")
public final class UpdateProductCommandDTO {
    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotBlank(message = "Primary barcode is required")
    private String primaryBarcode;

    @NotNull(message = "Unit of measure is required")
    private String unitOfMeasure;

    private List<String> secondaryBarcodes;
    private String category;
    private String brand;
}

