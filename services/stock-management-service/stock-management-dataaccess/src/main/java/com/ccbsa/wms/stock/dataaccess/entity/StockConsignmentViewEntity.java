package com.ccbsa.wms.stock.dataaccess.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity: StockConsignmentViewEntity
 * <p>
 * Read model entity for stock consignment queries. This is a denormalized view optimized for read operations.
 * <p>
 * Note: Currently maps to the stock_consignments table. In the future, this can be migrated to a separate
 * stock_consignment_views table that is maintained via event listeners for better read/write separation.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with
 * the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "stock_consignments", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class StockConsignmentViewEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", length = 255, nullable = false)
    private String tenantId;

    @Column(name = "consignment_reference", length = 100, nullable = false)
    private String consignmentReference;

    @Column(name = "warehouse_id", length = 50, nullable = false)
    private String warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private ConsignmentStatus status;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "received_by", length = 255)
    private String receivedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;

    @OneToMany(mappedBy = "consignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConsignmentLineItemEntity> lineItems = new ArrayList<>();
}

