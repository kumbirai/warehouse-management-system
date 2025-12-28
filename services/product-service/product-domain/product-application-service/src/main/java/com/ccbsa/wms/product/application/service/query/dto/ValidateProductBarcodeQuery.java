package com.ccbsa.wms.product.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: ValidateProductBarcodeQuery
 * <p>
 * Query object for validating a product barcode.
 */
@Getter
@Builder
public final class ValidateProductBarcodeQuery {
    private final String barcode;
    private final TenantId tenantId;

    public ValidateProductBarcodeQuery(String barcode, TenantId tenantId) {
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new IllegalArgumentException("Barcode is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        this.barcode = barcode;
        this.tenantId = tenantId;
    }
}

