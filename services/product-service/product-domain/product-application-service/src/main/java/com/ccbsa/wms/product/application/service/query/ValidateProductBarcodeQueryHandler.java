package com.ccbsa.wms.product.application.service.query;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.product.application.service.port.data.ProductViewRepository;
import com.ccbsa.wms.product.application.service.port.data.dto.ProductView;
import com.ccbsa.wms.product.application.service.query.dto.ProductInfo;
import com.ccbsa.wms.product.application.service.query.dto.ValidateProductBarcodeQuery;
import com.ccbsa.wms.product.application.service.query.dto.ValidateProductBarcodeResult;
import com.ccbsa.wms.product.domain.core.valueobject.BarcodeType;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;

/**
 * Query Handler: ValidateProductBarcodeQueryHandler
 * <p>
 * Handles product barcode validation queries.
 * <p>
 * Responsibilities:
 * - Validate barcode format
 * - Check cache for product information
 * - Look up product view by barcode from data port (read model)
 * - Cache product information for future lookups
 * - Return validation result with product info or error message
 * <p>
 * Uses data port (ProductViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@RequiredArgsConstructor
public class ValidateProductBarcodeQueryHandler {
    private final ProductViewRepository viewRepository;
    private final ProductBarcodeCache cache;

    @Transactional(readOnly = true)
    public ValidateProductBarcodeResult handle(ValidateProductBarcodeQuery query) {
        // 1. Validate barcode format
        BarcodeType barcodeFormat;
        try {
            ProductBarcode productBarcode = ProductBarcode.of(query.getBarcode());
            barcodeFormat = productBarcode.getType();
        } catch (IllegalArgumentException e) {
            return ValidateProductBarcodeResult.builder().valid(false).barcodeFormat(BarcodeType.CODE_128) // Default for invalid format
                    .errorMessage(String.format("Invalid barcode format: %s", e.getMessage())).build();
        }

        // 2. Check cache
        Optional<ProductInfo> cached = cache.get(query.getBarcode(), query.getTenantId());
        if (cached.isPresent()) {
            return ValidateProductBarcodeResult.builder().valid(true).productInfo(cached.get()).barcodeFormat(barcodeFormat).build();
        }

        // 3. Look up product view by barcode from data port
        Optional<ProductView> productView = viewRepository.findByTenantIdAndBarcode(query.getTenantId(), query.getBarcode());

        if (productView.isEmpty()) {
            return ValidateProductBarcodeResult.builder().valid(false).barcodeFormat(barcodeFormat)
                    .errorMessage(String.format("Product with barcode '%s' not found", query.getBarcode())).build();
        }

        // 4. Map to ProductInfo and cache
        ProductInfo productInfo = mapToProductInfo(productView.get(), query.getBarcode());
        cache.put(query.getBarcode(), query.getTenantId(), productInfo);

        return ValidateProductBarcodeResult.builder().valid(true).productInfo(productInfo).barcodeFormat(barcodeFormat).build();
    }

    /**
     * Maps ProductView read model to ProductInfo DTO.
     *
     * @param productView Product view (read model)
     * @param barcode     Barcode value that was used to find the product
     * @return ProductInfo DTO
     */
    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD", justification = "Method is called from handle() method on line 75 - SpotBugs false positive")
    private ProductInfo mapToProductInfo(ProductView productView, String barcode) {
        // Find the matching barcode (could be primary or secondary)
        ProductBarcode matchingBarcode = null;
        if (productView.getPrimaryBarcode().getValue().equals(barcode)) {
            matchingBarcode = productView.getPrimaryBarcode();
        } else {
            matchingBarcode = productView.getSecondaryBarcodes().stream().filter(b -> b.getValue().equals(barcode)).findFirst()
                    .orElse(productView.getPrimaryBarcode()); // Fallback to primary
        }

        return ProductInfo.builder().productId(productView.getProductId()).productCode(productView.getProductCode()).description(productView.getDescription())
                .barcode(matchingBarcode).build();
    }
}

