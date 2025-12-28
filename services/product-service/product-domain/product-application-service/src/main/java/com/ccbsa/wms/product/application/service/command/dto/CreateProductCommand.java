package com.ccbsa.wms.product.application.service.command.dto;

import java.util.List;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.product.domain.core.valueobject.UnitOfMeasure;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: CreateProductCommand
 * <p>
 * Command object for creating a new product.
 */
@Getter
@Builder
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Defensive copy made in static factory method.")
public final class CreateProductCommand {
    private final TenantId tenantId;
    private final ProductCode productCode;
    private final String description;
    private final ProductBarcode primaryBarcode;
    private final List<ProductBarcode> secondaryBarcodes;
    private final UnitOfMeasure unitOfMeasure;
    private final String category;
    private final String brand;

    /**
     * Static factory method with validation.
     */
    public static CreateProductCommand of(TenantId tenantId, ProductCode productCode, String description, ProductBarcode primaryBarcode, List<ProductBarcode> secondaryBarcodes,
                                          UnitOfMeasure unitOfMeasure, String category, String brand) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (productCode == null) {
            throw new IllegalArgumentException("ProductCode is required");
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
        return CreateProductCommand.builder().tenantId(tenantId).productCode(productCode).description(description).primaryBarcode(primaryBarcode)
                .secondaryBarcodes(secondaryBarcodes != null ? List.copyOf(secondaryBarcodes) : List.of()).unitOfMeasure(unitOfMeasure).category(category).brand(brand).build();
    }
}

