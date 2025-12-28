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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity: StockAllocationViewEntity
 * <p>
 * Read model entity for stock allocation queries. This is a denormalized view optimized for read operations.
 * <p>
 * Note: Currently maps to the stock_allocations table. In the future, this can be migrated to a separate
 * stock_allocation_views table that is maintained via event listeners for better read/write separation.
 */
@Entity
@Table(name = "stock_allocations", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class StockAllocationViewEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", length = 255, nullable = false)
    private String tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "stock_item_id", nullable = false)
    private UUID stockItemId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_type", length = 50, nullable = false)
    private AllocationType allocationType;

    @Column(name = "reference_id", length = 255)
    private String referenceId;

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
}

