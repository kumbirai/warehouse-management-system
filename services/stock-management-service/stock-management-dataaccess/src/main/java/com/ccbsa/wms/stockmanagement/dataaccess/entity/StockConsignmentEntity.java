package com.ccbsa.wms.stockmanagement.dataaccess.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ccbsa.wms.stockmanagement.domain.core.valueobject.ConsignmentStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * JPA Entity: StockConsignmentEntity
 * <p>
 * JPA representation of StockConsignment aggregate. Uses tenant schema resolver for multi-tenant isolation (schema-per-tenant strategy).
 * <p>
 * This entity maps to the StockConsignment domain aggregate and uses domain enums directly to maintain consistency between domain and persistence layers.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "stock_consignments",
        schema = "tenant_schema")
public class StockConsignmentEntity {
    @Id
    @Column(name = "id",
            nullable = false)
    private UUID id;

    @Column(name = "tenant_id",
            length = 255,
            nullable = false)
    private String tenantId;

    @Column(name = "consignment_reference",
            length = 100,
            nullable = false)
    private String consignmentReference;

    @Column(name = "warehouse_id",
            length = 50,
            nullable = false)
    private String warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status",
            length = 50,
            nullable = false)
    private ConsignmentStatus status;

    @Column(name = "received_at",
            nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "received_by",
            length = 255)
    private String receivedBy;

    @Column(name = "created_at",
            nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_at",
            nullable = false)
    private LocalDateTime lastModifiedAt;

    @Version
    @Column(name = "version",
            nullable = false)
    private Long version;

    @OneToMany(mappedBy = "consignment",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<ConsignmentLineItemEntity> lineItems = new ArrayList<>();

    // JPA requires no-arg constructor
    public StockConsignmentEntity() {
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getConsignmentReference() {
        return consignmentReference;
    }

    public void setConsignmentReference(String consignmentReference) {
        this.consignmentReference = consignmentReference;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public ConsignmentStatus getStatus() {
        return status;
    }

    public void setStatus(ConsignmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(String receivedBy) {
        this.receivedBy = receivedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<ConsignmentLineItemEntity> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<ConsignmentLineItemEntity> lineItems) {
        this.lineItems = lineItems;
    }
}

