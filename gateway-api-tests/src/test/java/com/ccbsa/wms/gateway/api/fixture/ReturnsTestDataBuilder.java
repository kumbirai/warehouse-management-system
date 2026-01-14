package com.ccbsa.wms.gateway.api.fixture;

import java.time.Instant;
import java.util.List;

import com.ccbsa.wms.gateway.api.dto.HandlePartialOrderAcceptanceRequest;
import com.ccbsa.wms.gateway.api.dto.ProcessFullOrderReturnRequest;
import com.ccbsa.wms.gateway.api.dto.RecordDamageAssessmentRequest;

/**
 * Builder for creating return test data.
 */
public class ReturnsTestDataBuilder {

    public static HandlePartialOrderAcceptanceRequest buildHandlePartialOrderAcceptanceRequest(String orderNumber, String productId) {
        return HandlePartialOrderAcceptanceRequest.builder().orderNumber(orderNumber).lineItems(
                        List.of(HandlePartialOrderAcceptanceRequest.PartialReturnLineItemRequest.builder().productId(productId).orderedQuantity(100).pickedQuantity(100)
                                .acceptedQuantity(80).returnReason("DEFECTIVE").lineNotes("Test partial return").build()))
                .signatureData("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==") // 1x1 PNG
                .signedAt(Instant.now()).build();
    }

    public static ProcessFullOrderReturnRequest buildProcessFullOrderReturnRequest(String orderNumber, String productId) {
        return ProcessFullOrderReturnRequest.builder().orderNumber(orderNumber).primaryReturnReason("DEFECTIVE").lineItems(
                List.of(ProcessFullOrderReturnRequest.FullReturnLineItemRequest.builder().productId(productId).orderedQuantity(100).pickedQuantity(100).productCondition("DAMAGED")
                        .returnReason("DEFECTIVE").lineNotes("Test full return").build())).returnNotes("Test full order return").build();
    }

    public static RecordDamageAssessmentRequest buildRecordDamageAssessmentRequest(String orderNumber, String productId) {
        return RecordDamageAssessmentRequest.builder().orderNumber(orderNumber).damageType("PHYSICAL").damageSeverity("SEVERE").damageSource("TRANSPORT").damagedProducts(
                List.of(RecordDamageAssessmentRequest.DamagedProductRequest.builder().productId(productId).damagedQuantity(10).photoUrl("https://example.com/damage-photo.jpg")
                        .notes("Product damaged during transport").build())).insuranceClaim(
                RecordDamageAssessmentRequest.InsuranceClaimRequest.builder().claimNumber("CLAIM-2025-001").insuranceCompany("ABC Insurance").claimStatus("PENDING")
                        .claimAmount(java.math.BigDecimal.valueOf(1000.00)).build()).damageNotes("Severe physical damage during transport").build();
    }
}

