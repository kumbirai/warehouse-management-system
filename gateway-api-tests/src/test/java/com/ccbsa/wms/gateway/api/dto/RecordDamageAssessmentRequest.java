package com.ccbsa.wms.gateway.api.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for recording damage assessment.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordDamageAssessmentRequest {
    private String orderNumber;
    private String damageType;
    private String damageSeverity;
    private String damageSource;
    private List<DamagedProductRequest> damagedProducts;
    private InsuranceClaimRequest insuranceClaim;
    private String damageNotes;

    @Getter
@Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DamagedProductRequest {
        private String productId;
        private Integer damagedQuantity;
        private String photoUrl;
        private String notes;
    }

    @Getter
@Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsuranceClaimRequest {
        private String claimNumber;
        private String insuranceCompany;
        private String claimStatus;
        private BigDecimal claimAmount;
    }
}
