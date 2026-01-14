package com.ccbsa.wms.returns.dataaccess.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ccbsa.common.domain.valueobject.DamageSeverity;
import com.ccbsa.common.domain.valueobject.DamageSource;
import com.ccbsa.common.domain.valueobject.DamageType;
import com.ccbsa.wms.returns.domain.core.valueobject.DamageAssessmentStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity: DamageAssessmentEntity
 * <p>
 * JPA representation of DamageAssessment aggregate. Uses tenant schema resolver for multi-tenant isolation (schema-per-tenant strategy).
 * <p>
 * This entity maps to the DamageAssessment domain aggregate and uses domain enums directly to maintain consistency between domain and persistence layers.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "damage_assessments", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class DamageAssessmentEntity {
    @Id
    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Column(name = "tenant_id", length = 255, nullable = false)
    private String tenantId;

    @Column(name = "order_number", length = 50, nullable = false)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "damage_type", length = 50, nullable = false)
    private DamageType damageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "damage_severity", length = 50, nullable = false)
    private DamageSeverity damageSeverity;

    @Enumerated(EnumType.STRING)
    @Column(name = "damage_source", length = 50, nullable = false)
    private DamageSource damageSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_status", length = 50, nullable = false)
    private DamageAssessmentStatus assessmentStatus;

    @Column(name = "insurance_claim_number", length = 100)
    private String insuranceClaimNumber;

    @Column(name = "insurance_company", length = 200)
    private String insuranceCompany;

    @Column(name = "insurance_claim_status", length = 50)
    private String insuranceClaimStatus;

    @Column(name = "insurance_claim_amount", precision = 19, scale = 2)
    private java.math.BigDecimal insuranceClaimAmount;

    @Column(name = "damage_notes", length = 2000)
    private String damageNotes;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "damageAssessment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DamagedProductItemEntity> damagedProductItems = new ArrayList<>();
}
