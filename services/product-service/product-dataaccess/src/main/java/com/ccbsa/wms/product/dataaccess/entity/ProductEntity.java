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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@Getter
@Setter
@NoArgsConstructor
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
}

