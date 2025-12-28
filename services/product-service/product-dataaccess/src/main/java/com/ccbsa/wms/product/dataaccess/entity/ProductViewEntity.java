package com.ccbsa.wms.product.dataaccess.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ccbsa.wms.product.domain.core.valueobject.BarcodeType;
import com.ccbsa.wms.product.domain.core.valueobject.UnitOfMeasure;

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
 * JPA Entity: ProductViewEntity
 * <p>
 * Read model entity for product queries. This is a denormalized view optimized for read operations.
 * <p>
 * Note: Currently maps to the products table. In the future, this can be migrated to a separate
 * product_views table that is maintained via event listeners for better read/write separation.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with
 * the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "products", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class ProductViewEntity {
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
}

