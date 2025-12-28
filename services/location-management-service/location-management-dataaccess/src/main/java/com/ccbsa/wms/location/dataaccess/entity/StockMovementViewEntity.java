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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity: StockMovementViewEntity
 * <p>
 * Read model entity for stock movement queries. This is a denormalized view optimized for read operations.
 * <p>
 * Note: Currently maps to the stock_movements table. In the future, this can be migrated to a separate
 * stock_movement_views table that is maintained via event listeners for better read/write separation.
 */
@Entity
@Table(name = "stock_movements", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class StockMovementViewEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", length = 255, nullable = false)
    private String tenantId;

    @Column(name = "stock_item_id", length = 255, nullable = false)
    private String stockItemId;

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
}

