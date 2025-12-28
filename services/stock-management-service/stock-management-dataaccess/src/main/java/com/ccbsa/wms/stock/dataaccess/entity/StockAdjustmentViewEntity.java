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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity: StockAdjustmentViewEntity
 * <p>
 * Read model entity for stock adjustment queries. This is a denormalized view optimized for read operations.
 * <p>
 * Note: Currently maps to the stock_adjustments table. In the future, this can be migrated to a separate
 * stock_adjustment_views table that is maintained via event listeners for better read/write separation.
 */
@Entity
@Table(name = "stock_adjustments", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class StockAdjustmentViewEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", length = 255, nullable = false)
    private String tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "stock_item_id")
    private UUID stockItemId;

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
    private String authorizationCode;

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
}

