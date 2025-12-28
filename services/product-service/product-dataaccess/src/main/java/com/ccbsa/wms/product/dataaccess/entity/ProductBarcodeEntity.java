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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity: ProductBarcodeEntity
 * <p>
 * JPA representation of secondary product barcodes.
 * <p>
 * This entity represents the one-to-many relationship between Product and secondary barcodes. Primary barcode is stored directly in ProductEntity.
 */
@Entity
@Table(name = "product_barcodes", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
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
}

