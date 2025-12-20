package com.ccbsa.wms.product.dataaccess.entity;

import java.util.UUID;

import com.ccbsa.wms.product.domain.core.valueobject.BarcodeType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA Entity: ProductBarcodeEntity
 * <p>
 * JPA representation of secondary product barcodes.
 * <p>
 * This entity represents the one-to-many relationship between Product and secondary barcodes. Primary barcode is stored directly in ProductEntity.
 */
@Entity
@Table(name = "product_barcodes", schema = "tenant_schema")
public class ProductBarcodeEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "barcode", length = 255, nullable = false)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "barcode_type", length = 50, nullable = false)
    private BarcodeType barcodeType;

    // JPA requires no-arg constructor
    public ProductBarcodeEntity() {
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public BarcodeType getBarcodeType() {
        return barcodeType;
    }

    public void setBarcodeType(BarcodeType barcodeType) {
        this.barcodeType = barcodeType;
    }
}

