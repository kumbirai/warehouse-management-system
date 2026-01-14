package com.ccbsa.wms.returns.application.dto.command;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.valueobject.DamageSeverity;
import com.ccbsa.common.domain.valueobject.DamageSource;
import com.ccbsa.common.domain.valueobject.DamageType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO: RecordDamageAssessmentResponseDTO
 * <p>
 * API response DTO for damage assessment recording.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for damage assessment recording")
public class RecordDamageAssessmentResponseDTO {
    @Schema(description = "Damage assessment ID", example = "950e8400-e29b-41d4-a716-446655440000")
    private String damageAssessmentId;

    @Schema(description = "Order number", example = "ORD-2025-001")
    private String orderNumber;

    @Schema(description = "Damage type", example = "PHYSICAL")
    private DamageType damageType;

    @Schema(description = "Damage severity", example = "SEVERE")
    private DamageSeverity damageSeverity;

    @Schema(description = "Damage source", example = "TRANSPORT")
    private DamageSource damageSource;

    @Schema(description = "Assessment status", example = "SUBMITTED")
    private String status;

    @Schema(description = "Recorded timestamp")
    private LocalDateTime recordedAt;
}
