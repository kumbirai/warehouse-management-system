package com.ccbsa.wms.product.application.service.query;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.product.application.service.port.repository.ProductRepository;
import com.ccbsa.wms.product.application.service.query.dto.ProductInfo;
import com.ccbsa.wms.product.application.service.query.dto.ValidateProductBarcodeQuery;
import com.ccbsa.wms.product.application.service.query.dto.ValidateProductBarcodeResult;
import com.ccbsa.wms.product.domain.core.entity.Product;
import com.ccbsa.wms.product.domain.core.valueobject.BarcodeType;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;

/**
 * Query Handler: ValidateProductBarcodeQueryHandler
 * <p>
 * Handles product barcode validation queries.
 * <p>
 * Responsibilities: - Validate barcode format - Check cache for product information - Look up product by barcode from repository - Cache product information for future lookups -
 * Return validation result with product info or error message
 */
@Component
public class ValidateProductBarcodeQueryHandler {
    private final ProductRepository repository;
    private final ProductBarcodeCache cache;

    public ValidateProductBarcodeQueryHandler(ProductRepository repository, ProductBarcodeCache cache) {
        this.repository = repository;
        this.cache = cache;
    }

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

        // 3. Look up product by barcode
        Optional<Product> product = repository.findByBarcodeAndTenantId(query.getBarcode(), query.getTenantId());

        if (product.isEmpty()) {
            return ValidateProductBarcodeResult.builder().valid(false).barcodeFormat(barcodeFormat)
                    .errorMessage(String.format("Product with barcode '%s' not found", query.getBarcode())).build();
        }

        // 4. Map to ProductInfo and cache
        ProductInfo productInfo = mapToProductInfo(product.get(), query.getBarcode());
        cache.put(query.getBarcode(), query.getTenantId(), productInfo);

        return ValidateProductBarcodeResult.builder().valid(true).productInfo(productInfo).barcodeFormat(barcodeFormat).build();
    }

    /**
     * Maps Product aggregate to ProductInfo DTO.
     *
     * @param product Product aggregate
     * @param barcode Barcode value that was used to find the product
     * @return ProductInfo DTO
     */
    private ProductInfo mapToProductInfo(Product product, String barcode) {
        // Find the matching barcode (could be primary or secondary)
        ProductBarcode matchingBarcode = null;
        if (product.getPrimaryBarcode().getValue().equals(barcode)) {
            matchingBarcode = product.getPrimaryBarcode();
        } else {
            matchingBarcode =
                    product.getSecondaryBarcodes().stream().filter(b -> b.getValue().equals(barcode)).findFirst().orElse(product.getPrimaryBarcode()); // Fallback to primary
        }

        return ProductInfo.builder().productId(product.getId()).productCode(product.getProductCode()).description(product.getDescription()).barcode(matchingBarcode).build();
    }
}

