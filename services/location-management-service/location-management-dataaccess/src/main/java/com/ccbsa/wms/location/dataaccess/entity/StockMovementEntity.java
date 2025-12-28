package com.ccbsa.wms.location.dataaccess.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ccbsa.common.domain.valueobject.MovementReason;
import com.ccbsa.wms.location.domain.core.valueobject.MovementStatus;
import com.ccbsa.wms.location.domain.core.valueobject.MovementType;

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
 * JPA Entity: StockMovementEntity
 * <p>
 * JPA representation of StockMovement aggregate. Uses tenant schema resolver for multi-tenant isolation (schema-per-tenant strategy).
 * <p>
 * This entity maps to the StockMovement domain aggregate and uses domain enums directly to maintain consistency between domain and persistence layers.
 * <p>
 * Note: StockMovement is tenant-aware. Uses schema-per-tenant strategy via TenantAwarePhysicalNamingStrategy
 * for multi-tenant isolation. Each tenant has its own isolated PostgreSQL schema.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with
 * the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "stock_movements", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class StockMovementEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", length = 255, nullable = false)
    private String tenantId;

    @Column(name = "stock_item_id", length = 255, nullable = false)
    private String stockItemId; // Cross-service reference (as String)

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "source_location_id", nullable = false)
    private UUID sourceLocationId;

    @Column(name = "destination_location_id", nullable = false)
    private UUID destinationLocationId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", length = 50, nullable = false)
    private MovementType movementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", length = 50, nullable = false)
    private MovementReason reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private MovementStatus status;

    @Column(name = "initiated_by", nullable = false)
    private UUID initiatedBy;

    @Column(name = "initiated_at", nullable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "completed_by")
    private UUID completedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}

