package com.ccbsa.wms.product.application.dto.query;

import com.ccbsa.wms.product.domain.core.valueobject.BarcodeType;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query Result DTO: ValidateProductBarcodeResultDTO
 * <p>
 * API response DTO for product barcode validation.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores DTO directly. DTOs are immutable when returned from API.")
public class ValidateProductBarcodeResultDTO {
    private boolean valid;
    private ProductInfoDTO productInfo;
    private BarcodeType barcodeFormat;
    private String errorMessage;

    /**
     * Nested DTO for product information.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductInfoDTO {
        private String productId;
        private String productCode;
        private String description;
        private String barcode;
        private BarcodeType barcodeType;
    }
}

