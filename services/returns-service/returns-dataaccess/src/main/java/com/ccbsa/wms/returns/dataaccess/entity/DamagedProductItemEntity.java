package com.ccbsa.wms.returns.dataaccess.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ccbsa.common.domain.valueobject.DamageSeverity;
import com.ccbsa.common.domain.valueobject.DamageSource;
import com.ccbsa.common.domain.valueobject.DamageType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity: DamagedProductItemEntity
 * <p>
 * JPA representation of DamagedProductItem entity. Uses tenant schema resolver for multi-tenant isolation (schema-per-tenant strategy).
 * <p>
 * This entity maps to the DamagedProductItem domain entity and represents a damaged product item within a damage assessment.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "damaged_product_items", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class DamagedProductItemEntity {
    @Id
    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @ManyToOne
    @JoinColumn(name = "assessment_id", nullable = false)
    private DamageAssessmentEntity damageAssessment;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "damaged_quantity", nullable = false)
    private int damagedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "damage_type", length = 50, nullable = false)
    private DamageType damageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "damage_severity", length = 50, nullable = false)
    private DamageSeverity damageSeverity;

    @Enumerated(EnumType.STRING)
    @Column(name = "damage_source", length = 50, nullable = false)
    private DamageSource damageSource;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
