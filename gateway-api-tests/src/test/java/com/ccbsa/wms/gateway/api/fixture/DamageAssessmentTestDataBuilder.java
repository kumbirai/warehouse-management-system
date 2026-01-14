package com.ccbsa.wms.gateway.api.fixture;

import java.math.BigDecimal;
import java.util.List;

import com.ccbsa.wms.gateway.api.dto.RecordDamageAssessmentRequest;

/**
 * Builder for creating damage assessment test data.
 */
public class DamageAssessmentTestDataBuilder {

    public static RecordDamageAssessmentRequest buildRecordDamageAssessmentRequest(String orderNumber, String productId) {
        return RecordDamageAssessmentRequest.builder().orderNumber(orderNumber).damageType("PHYSICAL").damageSeverity("MINOR").damageSource("HANDLING").damagedProducts(
                        List.of(RecordDamageAssessmentRequest.DamagedProductRequest.builder().productId(productId).damagedQuantity(5).notes("Minor damage during handling").build()))
                .damageNotes("Minor damage assessment").build();
    }

    public static RecordDamageAssessmentRequest buildRecordDamageAssessmentRequestWithInsurance(String orderNumber, String productId) {
        return RecordDamageAssessmentRequest.builder().orderNumber(orderNumber).damageType("PHYSICAL").damageSeverity("SEVERE").damageSource("TRANSPORT").damagedProducts(
                List.of(RecordDamageAssessmentRequest.DamagedProductRequest.builder().productId(productId).damagedQuantity(20).photoUrl("https://example.com/damage-photo.jpg")
                        .notes("Severe damage during transport").build())).insuranceClaim(
                RecordDamageAssessmentRequest.InsuranceClaimRequest.builder().claimNumber("CLAIM-" + TestData.faker().number().digits(6)).insuranceCompany("ABC Insurance")
                        .claimStatus("PENDING").claimAmount(BigDecimal.valueOf(5000.00)).build()).damageNotes("Severe damage requiring insurance claim").build();
    }
}
