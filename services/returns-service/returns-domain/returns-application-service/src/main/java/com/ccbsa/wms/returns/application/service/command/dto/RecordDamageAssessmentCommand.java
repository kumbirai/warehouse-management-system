package com.ccbsa.wms.returns.application.service.command.dto;

import java.util.List;

import com.ccbsa.common.domain.valueobject.DamageSeverity;
import com.ccbsa.common.domain.valueobject.DamageSource;
import com.ccbsa.common.domain.valueobject.DamageType;
import com.ccbsa.common.domain.valueobject.Notes;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.ProductCondition;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.returns.domain.core.valueobject.DamagedProductItemId;
import com.ccbsa.wms.returns.domain.core.valueobject.InsuranceClaimInfo;

import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: RecordDamageAssessmentCommand
 * <p>
 * Command object for recording damage assessments.
 */
@Getter
public final class RecordDamageAssessmentCommand {
    private final OrderNumber orderNumber;
    private final DamageType damageType;
    private final DamageSeverity damageSeverity;
    private final DamageSource damageSource;
    private final List<DamagedProductCommand> damagedProducts;
    private final InsuranceClaimInfo insuranceClaimInfo;
    private final Notes damageNotes;

    @Builder
    public RecordDamageAssessmentCommand(OrderNumber orderNumber, DamageType damageType, DamageSeverity damageSeverity, DamageSource damageSource,
                                         List<DamagedProductCommand> damagedProducts, InsuranceClaimInfo insuranceClaimInfo, Notes damageNotes) {
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
        if (damagedProducts == null || damagedProducts.isEmpty()) {
            throw new IllegalArgumentException("At least one damaged product is required");
        }
        // Validate insurance claim info for severe damage
        if ((damageSeverity == DamageSeverity.SEVERE || damageSeverity == DamageSeverity.TOTAL) && insuranceClaimInfo == null) {
            throw new IllegalArgumentException("Insurance claim info is required for severe or total damage");
        }
        this.orderNumber = orderNumber;
        this.damageType = damageType;
        this.damageSeverity = damageSeverity;
        this.damageSource = damageSource;
        this.damagedProducts = List.copyOf(damagedProducts);
        this.insuranceClaimInfo = insuranceClaimInfo;
        this.damageNotes = damageNotes;
    }

    @Getter
    public static final class DamagedProductCommand {
        private final DamagedProductItemId itemId;
        private final ProductId productId;
        private final Quantity damagedQuantity;
        private final DamageType damageType;
        private final DamageSeverity damageSeverity;
        private final DamageSource damageSource;
        private final String photoUrl;
        private final Notes notes;

        @Builder
        public DamagedProductCommand(DamagedProductItemId itemId, ProductId productId, Quantity damagedQuantity, DamageType damageType, DamageSeverity damageSeverity,
                                     DamageSource damageSource, String photoUrl, Notes notes) {
            if (itemId == null) {
                throw new IllegalArgumentException("ItemId is required");
            }
            if (productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (damagedQuantity == null || !damagedQuantity.isPositive()) {
                throw new IllegalArgumentException("Damaged quantity must be positive");
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
            if (photoUrl != null && photoUrl.length() > 500) {
                throw new IllegalArgumentException("Photo URL cannot exceed 500 characters");
            }
            this.itemId = itemId;
            this.productId = productId;
            this.damagedQuantity = damagedQuantity;
            this.damageType = damageType;
            this.damageSeverity = damageSeverity;
            this.damageSource = damageSource;
            this.photoUrl = photoUrl;
            this.notes = notes;
        }
    }
}
