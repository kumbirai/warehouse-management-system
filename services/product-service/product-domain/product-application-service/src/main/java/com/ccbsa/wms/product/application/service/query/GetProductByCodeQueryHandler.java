package com.ccbsa.wms.product.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.product.application.service.port.repository.ProductRepository;
import com.ccbsa.wms.product.application.service.query.dto.GetProductByCodeQuery;
import com.ccbsa.wms.product.application.service.query.dto.ProductQueryResult;
import com.ccbsa.wms.product.domain.core.exception.ProductNotFoundException;

/**
 * Query Handler: GetProductByCodeQueryHandler
 * <p>
 * Handles retrieval of Product aggregate by product code.
 * <p>
 * Responsibilities:
 * - Load Product aggregate from repository by product code
 * - Map aggregate to query result DTO
 * - Return optimized read model
 */
@Component
public class GetProductByCodeQueryHandler {

    private final ProductRepository repository;

    public GetProductByCodeQueryHandler(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ProductQueryResult handle(GetProductByCodeQuery query) {
        // 1. Load aggregate by product code
        com.ccbsa.wms.product.domain.core.entity.Product product = repository
                .findByProductCodeAndTenantId(query.getProductCode(), query.getTenantId())
                .orElseThrow(() -> new ProductNotFoundException(
                        String.format("Product not found with code: %s", query.getProductCode().getValue())));

        // 2. Map to query result
        return ProductQueryResult.builder()
                .productId(product.getId())
                .productCode(product.getProductCode())
                .description(product.getDescription())
                .primaryBarcode(product.getPrimaryBarcode())
                .secondaryBarcodes(product.getSecondaryBarcodes())
                .unitOfMeasure(product.getUnitOfMeasure())
                .category(product.getCategory())
                .brand(product.getBrand())
                .createdAt(product.getCreatedAt())
                .lastModifiedAt(product.getLastModifiedAt())
                .build();
    }
}

