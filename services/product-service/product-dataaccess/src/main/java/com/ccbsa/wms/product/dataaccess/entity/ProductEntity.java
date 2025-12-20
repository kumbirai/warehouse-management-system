package com.ccbsa.wms.product.dataaccess.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ccbsa.wms.product.domain.core.valueobject.BarcodeType;
import com.ccbsa.wms.product.domain.core.valueobject.UnitOfMeasure;

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
 * JPA Entity: ProductEntity
 * <p>
 * JPA representation of Product aggregate. Uses tenant schema resolver for multi-tenant isolation (schema-per-tenant strategy).
 * <p>
 * This entity maps to the Product domain aggregate and uses domain enums directly to maintain consistency between domain and persistence layers.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "products", schema = "tenant_schema")
public class ProductEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", length = 255, nullable = false)
    private String tenantId;

    @Column(name = "product_code", length = 100, nullable = false)
    private String productCode;

    @Column(name = "description", length = 500, nullable = false)
    private String description;

    @Column(name = "primary_barcode", length = 255, nullable = false)
    private String primaryBarcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_barcode_type", length = 50, nullable = false)
    private BarcodeType primaryBarcodeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_of_measure", length = 50, nullable = false)
    private UnitOfMeasure unitOfMeasure;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductBarcodeEntity> secondaryBarcodes = new ArrayList<>();

    // JPA requires no-arg constructor
    public ProductEntity() {
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

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrimaryBarcode() {
        return primaryBarcode;
    }

    public void setPrimaryBarcode(String primaryBarcode) {
        this.primaryBarcode = primaryBarcode;
    }

    public BarcodeType getPrimaryBarcodeType() {
        return primaryBarcodeType;
    }

    public void setPrimaryBarcodeType(BarcodeType primaryBarcodeType) {
        this.primaryBarcodeType = primaryBarcodeType;
    }

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
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

    public List<ProductBarcodeEntity> getSecondaryBarcodes() {
        return secondaryBarcodes;
    }

    public void setSecondaryBarcodes(List<ProductBarcodeEntity> secondaryBarcodes) {
        this.secondaryBarcodes = secondaryBarcodes;
    }
}

