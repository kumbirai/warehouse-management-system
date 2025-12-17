package com.ccbsa.wms.stockmanagement.dataaccess.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA Entity: ConsignmentLineItemEntity
 * <p>
 * JPA representation of ConsignmentLineItem value object. Uses tenant schema resolver for multi-tenant isolation (schema-per-tenant strategy).
 * <p>
 * This entity maps to the ConsignmentLineItem domain value object and represents a line item within a stock consignment.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "consignment_line_items",
        schema = "tenant_schema")
public class ConsignmentLineItemEntity {
    @Id
    @Column(name = "id",
            nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "consignment_id",
            nullable = false)
    private StockConsignmentEntity consignment;

    @Column(name = "product_code",
            length = 50,
            nullable = false)
    private String productCode;

    @Column(name = "quantity",
            nullable = false)
    private int quantity;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "created_at",
            nullable = false)
    private LocalDateTime createdAt;

    // JPA requires no-arg constructor
    public ConsignmentLineItemEntity() {
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public StockConsignmentEntity getConsignment() {
        return consignment;
    }

    public void setConsignment(StockConsignmentEntity consignment) {
        this.consignment = consignment;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

