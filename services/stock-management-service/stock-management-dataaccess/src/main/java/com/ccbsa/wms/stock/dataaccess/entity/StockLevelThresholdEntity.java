package com.ccbsa.wms.stock.dataaccess.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity: StockLevelThresholdEntity
 * <p>
 * JPA representation of StockLevelThreshold aggregate. Uses tenant schema resolver for multi-tenant isolation (schema-per-tenant strategy).
 * <p>
 * This entity maps to the StockLevelThreshold domain aggregate.
 * <p>
 * Note: StockLevelThreshold is tenant-aware. Uses schema-per-tenant strategy via TenantAwarePhysicalNamingStrategy
 * for multi-tenant isolation. Each tenant has its own isolated PostgreSQL schema.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with
 * the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "stock_level_thresholds", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class StockLevelThresholdEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", length = 255, nullable = false)
    private String tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "location_id")
    private UUID locationId; // NULL for warehouse-wide threshold

    @Column(name = "minimum_quantity", precision = 18, scale = 2)
    private BigDecimal minimumQuantity;

    @Column(name = "maximum_quantity", precision = 18, scale = 2)
    private BigDecimal maximumQuantity;

    @Column(name = "enable_auto_restock", nullable = false)
    private Boolean enableAutoRestock;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}

