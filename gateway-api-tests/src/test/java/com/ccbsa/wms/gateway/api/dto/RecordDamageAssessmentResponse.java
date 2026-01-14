package com.ccbsa.wms.gateway.api.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for damage assessment recording.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordDamageAssessmentResponse {
    private String damageAssessmentId;
    private String orderNumber;
    private String damageType;
    private String damageSeverity;
    private String damageSource;
    private String status;
    private LocalDateTime recordedAt;
}
