package com.ccbsa.wms.stock.dataaccess.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.ccbsa.common.domain.valueobject.StockClassification;

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
 * JPA Entity: StockItemViewEntity
 * <p>
 * Read model entity for stock item queries. This is a denormalized view optimized for read operations.
 * <p>
 * Note: Currently maps to the stock_items table. In the future, this can be migrated to a separate
 * stock_item_views table that is maintained via event listeners for better read/write separation.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with
 * the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "stock_items", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class StockItemViewEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", length = 255, nullable = false)
    private String tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "location_id")
    private UUID locationId; // May be null initially

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "allocated_quantity", nullable = false)
    private Integer allocatedQuantity; // Quantity allocated for picking orders

    @Column(name = "expiration_date")
    private LocalDate expirationDate; // May be null for non-perishable

    @Enumerated(EnumType.STRING)
    @Column(name = "classification", length = 50, nullable = false)
    private StockClassification classification;

    @Column(name = "consignment_id")
    private UUID consignmentId; // Reference to source consignment

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;
}

