package com.ccbsa.wms.product.domain.core.event;

import java.util.List;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductId;
import com.ccbsa.wms.product.domain.core.valueobject.UnitOfMeasure;

/**
 * Domain Event: ProductUpdatedEvent
 * <p>
 * Published when a product is updated.
 * <p>
 * Event Version: 1.0
 */
public final class ProductUpdatedEvent extends ProductEvent {
    private final ProductCode productCode;
    private final String description;
    private final ProductBarcode primaryBarcode;
    private final List<ProductBarcode> secondaryBarcodes;
    private final UnitOfMeasure unitOfMeasure;
    private final String category;
    private final String brand;
    private final TenantId tenantId;

    /**
     * Constructor for ProductUpdatedEvent without metadata.
     *
     * @param productId         Product identifier
     * @param tenantId          Tenant identifier
     * @param productCode       Product code
     * @param description       Product description
     * @param primaryBarcode    Primary barcode
     * @param secondaryBarcodes Secondary barcodes (can be null or empty)
     * @param unitOfMeasure     Unit of measure
     * @param category          Product category (optional)
     * @param brand             Product brand (optional)
     * @throws IllegalArgumentException if any required parameter is null
     */
    public ProductUpdatedEvent(ProductId productId, TenantId tenantId, ProductCode productCode,
                               String description, ProductBarcode primaryBarcode,
                               List<ProductBarcode> secondaryBarcodes, UnitOfMeasure unitOfMeasure,
                               String category, String brand) {
        super(productId);
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId cannot be null");
        }
        if (productCode == null) {
            throw new IllegalArgumentException("ProductCode cannot be null");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        if (primaryBarcode == null) {
            throw new IllegalArgumentException("PrimaryBarcode cannot be null");
        }
        if (unitOfMeasure == null) {
            throw new IllegalArgumentException("UnitOfMeasure cannot be null");
        }
        this.tenantId = tenantId;
        this.productCode = productCode;
        this.description = description;
        this.primaryBarcode = primaryBarcode;
        this.secondaryBarcodes = secondaryBarcodes != null ? List.copyOf(secondaryBarcodes) : List.of();
        this.unitOfMeasure = unitOfMeasure;
        this.category = category;
        this.brand = brand;
    }

    /**
     * Constructor for ProductUpdatedEvent with metadata.
     *
     * @param productId         Product identifier
     * @param tenantId          Tenant identifier
     * @param productCode       Product code
     * @param description       Product description
     * @param primaryBarcode    Primary barcode
     * @param secondaryBarcodes Secondary barcodes (can be null or empty)
     * @param unitOfMeasure     Unit of measure
     * @param category          Product category (optional)
     * @param brand             Product brand (optional)
     * @param metadata          Event metadata for traceability
     * @throws IllegalArgumentException if any required parameter is null
     */
    public ProductUpdatedEvent(ProductId productId, TenantId tenantId, ProductCode productCode,
                               String description, ProductBarcode primaryBarcode,
                               List<ProductBarcode> secondaryBarcodes, UnitOfMeasure unitOfMeasure,
                               String category, String brand, EventMetadata metadata) {
        super(productId, metadata);
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId cannot be null");
        }
        if (productCode == null) {
            throw new IllegalArgumentException("ProductCode cannot be null");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        if (primaryBarcode == null) {
            throw new IllegalArgumentException("PrimaryBarcode cannot be null");
        }
        if (unitOfMeasure == null) {
            throw new IllegalArgumentException("UnitOfMeasure cannot be null");
        }
        this.tenantId = tenantId;
        this.productCode = productCode;
        this.description = description;
        this.primaryBarcode = primaryBarcode;
        this.secondaryBarcodes = secondaryBarcodes != null ? List.copyOf(secondaryBarcodes) : List.of();
        this.unitOfMeasure = unitOfMeasure;
        this.category = category;
        this.brand = brand;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public ProductCode getProductCode() {
        return productCode;
    }

    public String getDescription() {
        return description;
    }

    public ProductBarcode getPrimaryBarcode() {
        return primaryBarcode;
    }

    public List<ProductBarcode> getSecondaryBarcodes() {
        return secondaryBarcodes;
    }

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public String getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }
}

