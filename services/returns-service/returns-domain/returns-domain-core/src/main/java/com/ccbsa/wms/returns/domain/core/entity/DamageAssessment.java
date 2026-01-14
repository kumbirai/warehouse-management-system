package com.ccbsa.wms.returns.domain.core.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.common.domain.valueobject.DamageSeverity;
import com.ccbsa.common.domain.valueobject.DamageSource;
import com.ccbsa.common.domain.valueobject.DamageType;
import com.ccbsa.common.domain.valueobject.Notes;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.domain.core.event.DamageRecordedEvent;
import com.ccbsa.wms.returns.domain.core.valueobject.DamageAssessmentId;
import com.ccbsa.wms.returns.domain.core.valueobject.DamageAssessmentStatus;
import com.ccbsa.wms.returns.domain.core.valueobject.InsuranceClaimInfo;

/**
 * Aggregate Root: DamageAssessment
 * <p>
 * Represents a damage assessment for products damaged in transit.
 * <p>
 * Business Rules:
 * - Damage assessment must have at least one damaged product item
 * - Insurance claim info optional but required if damage severity is SEVERE or TOTAL
 * - Status transitions: SUBMITTED → UNDER_REVIEW → COMPLETED
 */
public class DamageAssessment extends TenantAwareAggregateRoot<DamageAssessmentId> {
    private OrderNumber orderNumber;
    private DamageType damageType;
    private DamageSeverity damageSeverity;
    private DamageSource damageSource;
    private List<DamagedProductItem> damagedProductItems;
    private InsuranceClaimInfo insuranceClaimInfo;
    private DamageAssessmentStatus status;
    private Notes damageNotes;
    private LocalDateTime recordedAt;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    /**
     * Private constructor for builder pattern. Prevents direct instantiation.
     */
    private DamageAssessment() {
        this.damagedProductItems = new ArrayList<>();
    }

    /**
     * Factory method to record a damage assessment.
     *
     * @param damageAssessmentId  Damage assessment ID
     * @param orderNumber         Order number
     * @param tenantId            Tenant ID
     * @param damageType          Damage type
     * @param damageSeverity      Damage severity
     * @param damageSource        Damage source
     * @param damagedProductItems List of damaged product items
     * @param insuranceClaimInfo  Optional insurance claim info
     * @param damageNotes         Optional damage notes
     * @return DamageAssessment aggregate instance
     * @throws IllegalArgumentException if validation fails
     */
    public static DamageAssessment recordDamage(DamageAssessmentId damageAssessmentId, OrderNumber orderNumber, TenantId tenantId, DamageType damageType,
                                                DamageSeverity damageSeverity, DamageSource damageSource, List<DamagedProductItem> damagedProductItems,
                                                InsuranceClaimInfo insuranceClaimInfo, Notes damageNotes) {
        if (damageAssessmentId == null) {
            throw new IllegalArgumentException("Damage assessment ID cannot be null");
        }
        if (orderNumber == null) {
            throw new IllegalArgumentException("Order number cannot be null");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        if (damageType == null) {
            throw new IllegalArgumentException("Damage type cannot be null");
        }
        if (damageSeverity == null) {
            throw new IllegalArgumentException("Damage severity cannot be null");
        }
        if (damageSource == null) {
            throw new IllegalArgumentException("Damage source cannot be null");
        }
        if (damagedProductItems == null || damagedProductItems.isEmpty()) {
            throw new IllegalArgumentException("Damage assessment must have at least one damaged product item");
        }

        // Validate insurance claim info for severe damage
        if ((damageSeverity == DamageSeverity.SEVERE || damageSeverity == DamageSeverity.TOTAL) && insuranceClaimInfo == null) {
            throw new IllegalArgumentException("Insurance claim info is required for severe or total damage");
        }

        Notes notes = damageNotes != null ? damageNotes : Notes.of(null);

        LocalDateTime now = LocalDateTime.now();
        DamageAssessment damageAssessment =
                DamageAssessment.builder().damageAssessmentId(damageAssessmentId).orderNumber(orderNumber).tenantId(tenantId).damageType(damageType).damageSeverity(damageSeverity)
                        .damageSource(damageSource).damagedProductItems(new ArrayList<>(damagedProductItems)).insuranceClaimInfo(insuranceClaimInfo)
                        .status(DamageAssessmentStatus.SUBMITTED).damageNotes(notes).recordedAt(now).createdAt(now).lastModifiedAt(now).build();

        // Publish domain event
        damageAssessment.addDomainEvent(new DamageRecordedEvent(damageAssessment.getId().getValueAsString(), orderNumber, tenantId, damageType, damageSeverity, damageSource,
                damageAssessment.damagedProductItems));

        return damageAssessment;
    }

    /**
     * Factory method to create builder instance.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Business logic method: Completes the damage assessment.
     * <p>
     * Business Rules:
     * - Can only complete assessments in UNDER_REVIEW status
     * - Sets status to COMPLETED
     *
     * @throws IllegalStateException if assessment is not in UNDER_REVIEW status
     */
    public void completeAssessment() {
        if (this.status != DamageAssessmentStatus.UNDER_REVIEW) {
            throw new IllegalStateException(String.format("Cannot complete damage assessment in status: %s. Only UNDER_REVIEW assessments can be completed.", this.status));
        }

        this.status = DamageAssessmentStatus.COMPLETED;
        this.lastModifiedAt = LocalDateTime.now();
        incrementVersion();

        // Publish domain event (DamageAssessmentCompletedEvent would be published here)
    }

    /**
     * Business logic method: Updates insurance claim information.
     *
     * @param insuranceClaimInfo Insurance claim info
     * @throws IllegalArgumentException if insuranceClaimInfo is null
     */
    public void updateInsuranceClaim(InsuranceClaimInfo insuranceClaimInfo) {
        if (insuranceClaimInfo == null) {
            throw new IllegalArgumentException("Insurance claim info cannot be null");
        }

        this.insuranceClaimInfo = insuranceClaimInfo;
        this.lastModifiedAt = LocalDateTime.now();
        incrementVersion();

        // Publish domain event (InsuranceClaimUpdatedEvent would be published here)
    }

    // Getters (read-only access)

    public OrderNumber getOrderNumber() {
        return orderNumber;
    }

    public DamageType getDamageType() {
        return damageType;
    }

    public DamageSeverity getDamageSeverity() {
        return damageSeverity;
    }

    public DamageSource getDamageSource() {
        return damageSource;
    }

    public List<DamagedProductItem> getDamagedProductItems() {
        return Collections.unmodifiableList(damagedProductItems);
    }

    public InsuranceClaimInfo getInsuranceClaimInfo() {
        return insuranceClaimInfo;
    }

    public DamageAssessmentStatus getStatus() {
        return status;
    }

    public Notes getDamageNotes() {
        return damageNotes;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    /**
     * Builder class for constructing DamageAssessment instances. Ensures all required fields are set and validated.
     */
    public static class Builder {
        private DamageAssessment damageAssessment = new DamageAssessment();

        public Builder damageAssessmentId(DamageAssessmentId id) {
            damageAssessment.setId(id);
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            damageAssessment.setTenantId(tenantId);
            return this;
        }

        public Builder orderNumber(OrderNumber orderNumber) {
            damageAssessment.orderNumber = orderNumber;
            return this;
        }

        public Builder damageType(DamageType damageType) {
            damageAssessment.damageType = damageType;
            return this;
        }

        public Builder damageSeverity(DamageSeverity damageSeverity) {
            damageAssessment.damageSeverity = damageSeverity;
            return this;
        }

        public Builder damageSource(DamageSource damageSource) {
            damageAssessment.damageSource = damageSource;
            return this;
        }

        public Builder damagedProductItems(List<DamagedProductItem> damagedProductItems) {
            if (damagedProductItems != null) {
                damageAssessment.damagedProductItems = new ArrayList<>(damagedProductItems);
            }
            return this;
        }

        public Builder insuranceClaimInfo(InsuranceClaimInfo insuranceClaimInfo) {
            damageAssessment.insuranceClaimInfo = insuranceClaimInfo;
            return this;
        }

        public Builder status(DamageAssessmentStatus status) {
            damageAssessment.status = status;
            return this;
        }

        public Builder damageNotes(Notes damageNotes) {
            damageAssessment.damageNotes = damageNotes;
            return this;
        }

        public Builder recordedAt(LocalDateTime recordedAt) {
            damageAssessment.recordedAt = recordedAt;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            damageAssessment.createdAt = createdAt;
            return this;
        }

        public Builder lastModifiedAt(LocalDateTime lastModifiedAt) {
            damageAssessment.lastModifiedAt = lastModifiedAt;
            return this;
        }

        public Builder version(int version) {
            damageAssessment.setVersion(version);
            return this;
        }

        /**
         * Builds and validates the DamageAssessment instance.
         *
         * @return Validated DamageAssessment instance
         * @throws IllegalArgumentException if validation fails
         */
        public DamageAssessment build() {
            validate();
            initializeDefaults();
            return consumeDamageAssessment();
        }

        /**
         * Validates all required fields are set.
         *
         * @throws IllegalArgumentException if validation fails
         */
        private void validate() {
            if (damageAssessment.getId() == null) {
                throw new IllegalArgumentException("DamageAssessmentId is required");
            }
            if (damageAssessment.getTenantId() == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (damageAssessment.orderNumber == null) {
                throw new IllegalArgumentException("OrderNumber is required");
            }
            if (damageAssessment.damageType == null) {
                throw new IllegalArgumentException("DamageType is required");
            }
            if (damageAssessment.damageSeverity == null) {
                throw new IllegalArgumentException("DamageSeverity is required");
            }
            if (damageAssessment.damageSource == null) {
                throw new IllegalArgumentException("DamageSource is required");
            }
            if (damageAssessment.damagedProductItems == null || damageAssessment.damagedProductItems.isEmpty()) {
                throw new IllegalArgumentException("At least one damaged product item is required");
            }
            if (damageAssessment.status == null) {
                throw new IllegalArgumentException("DamageAssessmentStatus is required");
            }
        }

        /**
         * Initializes default values for optional fields.
         */
        private void initializeDefaults() {
            if (damageAssessment.createdAt == null) {
                damageAssessment.createdAt = LocalDateTime.now();
            }
            if (damageAssessment.lastModifiedAt == null) {
                damageAssessment.lastModifiedAt = LocalDateTime.now();
            }
            if (damageAssessment.recordedAt == null) {
                damageAssessment.recordedAt = LocalDateTime.now();
            }
        }

        /**
         * Consumes the damage assessment from the builder and returns it. Creates a new damage assessment instance for the next build.
         *
         * @return Built damage assessment
         */
        private DamageAssessment consumeDamageAssessment() {
            DamageAssessment builtDamageAssessment = damageAssessment;
            damageAssessment = new DamageAssessment();
            return builtDamageAssessment;
        }

        /**
         * Builds DamageAssessment without publishing creation event. Used when reconstructing from persistence.
         *
         * @return Validated DamageAssessment instance
         * @throws IllegalArgumentException if validation fails
         */
        public DamageAssessment buildWithoutEvents() {
            validate();
            initializeDefaults();
            return consumeDamageAssessment();
        }
    }
}
