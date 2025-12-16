package com.ccbsa.wms.product.application.dto.query;

/**
 * Query Result DTO: ProductCodeUniquenessResultDTO
 * <p>
 * API response DTO for product code uniqueness check.
 */
public final class ProductCodeUniquenessResultDTO {
    private String productCode;
    private boolean isUnique;

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public boolean isUnique() {
        return isUnique;
    }

    public void setUnique(boolean unique) {
        isUnique = unique;
    }
}

