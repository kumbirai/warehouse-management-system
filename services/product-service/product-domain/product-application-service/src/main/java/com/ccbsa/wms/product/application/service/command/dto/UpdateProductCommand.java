package com.ccbsa.wms.product.application.service.command.dto;

import java.util.List;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.UnitOfMeasure;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: UpdateProductCommand
 * <p>
 * Command object for updating an existing product.
 */
@Getter
@Builder
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Defensive copy made in static factory method.")
public final class UpdateProductCommand {
    private final ProductId productId;
    private final TenantId tenantId;
    private final String description;
    private final ProductBarcode primaryBarcode;
    private final List<ProductBarcode> secondaryBarcodes;
    private final UnitOfMeasure unitOfMeasure;
    private final String category;
    private final String brand;

    /**
     * Static factory method with validation.
     */
    public static UpdateProductCommand of(ProductId productId, TenantId tenantId, String description, ProductBarcode primaryBarcode, List<ProductBarcode> secondaryBarcodes,
                                          UnitOfMeasure unitOfMeasure, String category, String brand) {
        if (productId == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (primaryBarcode == null) {
            throw new IllegalArgumentException("PrimaryBarcode is required");
        }
        if (unitOfMeasure == null) {
            throw new IllegalArgumentException("UnitOfMeasure is required");
        }
        return UpdateProductCommand.builder().productId(productId).tenantId(tenantId).description(description).primaryBarcode(primaryBarcode)
                .secondaryBarcodes(secondaryBarcodes != null ? List.copyOf(secondaryBarcodes) : List.of()).unitOfMeasure(unitOfMeasure).category(category).brand(brand).build();
    }
}

