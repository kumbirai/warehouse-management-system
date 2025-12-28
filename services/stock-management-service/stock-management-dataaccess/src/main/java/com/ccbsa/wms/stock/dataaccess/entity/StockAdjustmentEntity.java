package com.ccbsa.wms.stock.dataaccess.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ccbsa.common.domain.valueobject.AdjustmentReason;
import com.ccbsa.common.domain.valueobject.AdjustmentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity: StockAdjustmentEntity
 * <p>
 * JPA representation of StockAdjustment aggregate. Uses tenant schema resolver for multi-tenant isolation (schema-per-tenant strategy).
 * <p>
 * This entity maps to the StockAdjustment domain aggregate and uses domain enums directly to maintain consistency between domain and persistence layers.
 * <p>
 * Note: StockAdjustment is tenant-aware. Uses schema-per-tenant strategy via TenantAwarePhysicalNamingStrategy
 * for multi-tenant isolation. Each tenant has its own isolated PostgreSQL schema.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with
 * the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "stock_adjustments", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class StockAdjustmentEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", length = 255, nullable = false)
    private String tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "location_id")
    private UUID locationId; // Optional - null for product-wide adjustment

    @Column(name = "stock_item_id")
    private UUID stockItemId; // Optional - null for product/location adjustment

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", length = 50, nullable = false)
    private AdjustmentType adjustmentType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", length = 50, nullable = false)
    private AdjustmentReason reason;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "adjusted_by", nullable = false)
    private UUID adjustedBy;

    @Column(name = "authorization_code", length = 255)
    private String authorizationCode; // For large adjustments

    @Column(name = "adjusted_at", nullable = false)
    private LocalDateTime adjustedAt;

    @Column(name = "quantity_before", nullable = false)
    private Integer quantityBefore;

    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}

