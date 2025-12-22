package com.ccbsa.wms.product.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.product.application.service.port.repository.ProductRepository;
import com.ccbsa.wms.product.application.service.query.dto.GetProductQuery;
import com.ccbsa.wms.product.application.service.query.dto.ProductQueryResult;
import com.ccbsa.wms.product.domain.core.entity.Product;
import com.ccbsa.wms.product.domain.core.exception.ProductNotFoundException;

/**
 * Query Handler: GetProductQueryHandler
 * <p>
 * Handles retrieval of Product aggregate by ID.
 * <p>
 * Responsibilities: - Load Product aggregate from repository - Map aggregate to query result DTO - Return optimized read model
 */
@Component
public class GetProductQueryHandler {

    private final ProductRepository repository;

    public GetProductQueryHandler(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ProductQueryResult handle(GetProductQuery query) {
        // 1. Load aggregate
        Product product = repository.findByIdAndTenantId(query.getProductId(), query.getTenantId())
                .orElseThrow(() -> new ProductNotFoundException(String.format("Product not found: %s", query.getProductId().getValueAsString())));

        // 2. Map to query result
        return ProductQueryResult.builder().productId(product.getId()).productCode(product.getProductCode()).description(product.getDescription().getValue())
                .primaryBarcode(product.getPrimaryBarcode()).secondaryBarcodes(product.getSecondaryBarcodes()).unitOfMeasure(product.getUnitOfMeasure())
                .category(product.getCategory() != null ? product.getCategory().getValue() : null).brand(product.getBrand() != null ? product.getBrand().getValue() : null)
                .createdAt(product.getCreatedAt()).lastModifiedAt(product.getLastModifiedAt()).build();
    }
}

