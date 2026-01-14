package com.ccbsa.wms.returns.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.valueobject.DamageSeverity;
import com.ccbsa.common.domain.valueobject.DamageSource;
import com.ccbsa.common.domain.valueobject.DamageType;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.wms.returns.domain.core.valueobject.DamageAssessmentId;
import com.ccbsa.wms.returns.domain.core.valueobject.DamageAssessmentStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * Command Result DTO: RecordDamageAssessmentResult
 * <p>
 * Result object returned from RecordDamageAssessmentCommand execution.
 */
@Getter
@Builder
public final class RecordDamageAssessmentResult {
    private final DamageAssessmentId damageAssessmentId;
    private final OrderNumber orderNumber;
    private final DamageType damageType;
    private final DamageSeverity damageSeverity;
    private final DamageSource damageSource;
    private final DamageAssessmentStatus status;
    private final LocalDateTime recordedAt;

    public RecordDamageAssessmentResult(DamageAssessmentId damageAssessmentId, OrderNumber orderNumber, DamageType damageType, DamageSeverity damageSeverity,
                                        DamageSource damageSource, DamageAssessmentStatus status, LocalDateTime recordedAt) {
        if (damageAssessmentId == null) {
            throw new IllegalArgumentException("DamageAssessmentId is required");
        }
        if (orderNumber == null) {
            throw new IllegalArgumentException("OrderNumber is required");
        }
        if (damageType == null) {
            throw new IllegalArgumentException("DamageType is required");
        }
        if (damageSeverity == null) {
            throw new IllegalArgumentException("DamageSeverity is required");
        }
        if (damageSource == null) {
            throw new IllegalArgumentException("DamageSource is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        if (recordedAt == null) {
            throw new IllegalArgumentException("RecordedAt is required");
        }
        this.damageAssessmentId = damageAssessmentId;
        this.orderNumber = orderNumber;
        this.damageType = damageType;
        this.damageSeverity = damageSeverity;
        this.damageSource = damageSource;
        this.status = status;
        this.recordedAt = recordedAt;
    }
}
