package com.ccbsa.wms.stock.dataaccess.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ccbsa.common.domain.valueobject.AllocationType;
import com.ccbsa.wms.stock.domain.core.valueobject.AllocationStatus;

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
 * JPA Entity: StockAllocationEntity
 * <p>
 * JPA representation of StockAllocation aggregate. Uses tenant schema resolver for multi-tenant isolation (schema-per-tenant strategy).
 * <p>
 * This entity maps to the StockAllocation domain aggregate and uses domain enums directly to maintain consistency between domain and persistence layers.
 * <p>
 * Note: StockAllocation is tenant-aware. Uses schema-per-tenant strategy via TenantAwarePhysicalNamingStrategy
 * for multi-tenant isolation. Each tenant has its own isolated PostgreSQL schema.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with
 * the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "stock_allocations", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class StockAllocationEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", length = 255, nullable = false)
    private String tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "location_id")
    private UUID locationId; // Optional - null for product-wide allocation

    @Column(name = "stock_item_id", nullable = false)
    private UUID stockItemId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_type", length = 50, nullable = false)
    private AllocationType allocationType;

    @Column(name = "reference_id", length = 255)
    private String referenceId; // Order ID, picking list ID, etc.

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AllocationStatus status;

    @Column(name = "allocated_by", nullable = false)
    private UUID allocatedBy;

    @Column(name = "allocated_at", nullable = false)
    private LocalDateTime allocatedAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}

