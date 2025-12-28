package com.ccbsa.wms.product.application.service.port.data.dto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.product.domain.core.valueobject.UnitOfMeasure;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Read Model DTO: ProductView
 * <p>
 * Optimized read model representation of Product aggregate for query operations.
 * <p>
 * This is a denormalized view optimized for read queries, separate from the write model (Product aggregate).
 * <p>
 * Fields are flattened and optimized for query performance.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Defensive copy made in constructor and getter returns immutable view.")
public final class ProductView {
    private final ProductId productId;
    private final String tenantId;
    private final ProductCode productCode;
    private final String description;
    private final ProductBarcode primaryBarcode;
    private final List<ProductBarcode> secondaryBarcodes;
    private final UnitOfMeasure unitOfMeasure;
    private final String category;
    private final String brand;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastModifiedAt;

    public ProductView(ProductId productId, String tenantId, ProductCode productCode, String description, ProductBarcode primaryBarcode, List<ProductBarcode> secondaryBarcodes,
                       UnitOfMeasure unitOfMeasure, String category, String brand, LocalDateTime createdAt, LocalDateTime lastModifiedAt) {
        if (productId == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (productCode == null) {
            throw new IllegalArgumentException("ProductCode is required");
        }
        if (description == null) {
            throw new IllegalArgumentException("Description is required");
        }
        if (primaryBarcode == null) {
            throw new IllegalArgumentException("PrimaryBarcode is required");
        }
        if (unitOfMeasure == null) {
            throw new IllegalArgumentException("UnitOfMeasure is required");
        }
        this.productId = productId;
        this.tenantId = tenantId;
        this.productCode = productCode;
        this.description = description;
        this.primaryBarcode = primaryBarcode;
        // Defensive copy to prevent external modification
        this.secondaryBarcodes = secondaryBarcodes != null ? List.copyOf(secondaryBarcodes) : List.of();
        this.unitOfMeasure = unitOfMeasure;
        this.category = category;
        this.brand = brand;
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
    }

    /**
     * Returns an unmodifiable view of the secondary barcodes list.
     *
     * @return Unmodifiable list of secondary barcodes
     */
    public List<ProductBarcode> getSecondaryBarcodes() {
        return Collections.unmodifiableList(secondaryBarcodes);
    }
}

