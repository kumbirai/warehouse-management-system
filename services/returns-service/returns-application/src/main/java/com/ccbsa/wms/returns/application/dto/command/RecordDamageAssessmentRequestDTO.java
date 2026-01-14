package com.ccbsa.wms.returns.application.dto.command;

import java.math.BigDecimal;
import java.util.List;

import com.ccbsa.common.domain.valueobject.DamageSeverity;
import com.ccbsa.common.domain.valueobject.DamageSource;
import com.ccbsa.common.domain.valueobject.DamageType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO: RecordDamageAssessmentRequestDTO
 * <p>
 * API request DTO for recording damage assessments.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to record damage assessment")
public class RecordDamageAssessmentRequestDTO {
    @NotBlank(message = "Order number is required")
    @Schema(description = "Order number", example = "ORD-2025-001", required = true)
    private String orderNumber;

    @NotNull(message = "Damage type is required")
    @Schema(description = "Damage type", example = "PHYSICAL", required = true)
    private DamageType damageType;

    @NotNull(message = "Damage severity is required")
    @Schema(description = "Damage severity", example = "SEVERE", required = true)
    private DamageSeverity damageSeverity;

    @NotNull(message = "Damage source is required")
    @Schema(description = "Damage source", example = "TRANSPORT", required = true)
    private DamageSource damageSource;

    @NotEmpty(message = "At least one damaged product is required")
    @Valid
    @Schema(description = "Damaged products", required = true)
    private List<DamagedProductRequestDTO> damagedProducts;

    @Valid
    @Schema(description = "Insurance claim information (required for severe damage)")
    private InsuranceClaimRequestDTO insuranceClaim;

    @Size(max = 2000, message = "Damage notes cannot exceed 2000 characters")
    @Schema(description = "Optional damage notes")
    private String damageNotes;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Damaged product request")
    public static class DamagedProductRequestDTO {
        @NotBlank(message = "Product ID is required")
        @Schema(description = "Product ID", example = "750e8400-e29b-41d4-a716-446655440000", required = true)
        private String productId;

        @NotNull(message = "Damaged quantity is required")
        @Schema(description = "Damaged quantity", example = "10", required = true)
        private Integer damagedQuantity;

        @Schema(description = "Photo URL", example = "https://example.com/photo.jpg")
        private String photoUrl;

        @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
        @Schema(description = "Optional notes")
        private String notes;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Insurance claim request")
    public static class InsuranceClaimRequestDTO {
        @NotBlank(message = "Claim number is required")
        @Schema(description = "Insurance claim number", example = "CLAIM-2025-001", required = true)
        private String claimNumber;

        @NotBlank(message = "Insurance company is required")
        @Schema(description = "Insurance company name", example = "ABC Insurance", required = true)
        private String insuranceCompany;

        @Schema(description = "Claim status", example = "PENDING")
        private String claimStatus;

        @Schema(description = "Claim amount", example = "1000.00")
        private BigDecimal claimAmount;
    }
}
