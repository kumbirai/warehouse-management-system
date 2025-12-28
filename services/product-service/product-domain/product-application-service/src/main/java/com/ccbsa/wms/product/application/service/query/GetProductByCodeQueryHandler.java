package com.ccbsa.wms.product.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.product.application.service.port.data.ProductViewRepository;
import com.ccbsa.wms.product.application.service.query.dto.GetProductByCodeQuery;
import com.ccbsa.wms.product.application.service.query.dto.ProductQueryResult;
import com.ccbsa.wms.product.domain.core.exception.ProductNotFoundException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;

/**
 * Query Handler: GetProductByCodeQueryHandler
 * <p>
 * Handles retrieval of Product read model by product code.
 * <p>
 * Responsibilities:
 * - Load Product view from data port (read model) by product code
 * - Map view to query result DTO
 * - Return optimized read model
 * <p>
 * Uses data port (ProductViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@RequiredArgsConstructor
public class GetProductByCodeQueryHandler {
    private final ProductViewRepository viewRepository;

    @Transactional(readOnly = true)
    @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE", justification = "productView is used in builder chain - SpotBugs false positive with var keyword")
    public ProductQueryResult handle(GetProductByCodeQuery query) {
        // 1. Load read model (view) from data port by product code
        var productView = viewRepository.findByTenantIdAndProductCode(query.getTenantId(), query.getProductCode().getValue())
                .orElseThrow(() -> new ProductNotFoundException(String.format("Product not found with code: %s", query.getProductCode().getValue())));

        // 2. Map view to query result
        return ProductQueryResult.builder().productId(productView.getProductId()).productCode(productView.getProductCode()).description(productView.getDescription())
                .primaryBarcode(productView.getPrimaryBarcode()).secondaryBarcodes(productView.getSecondaryBarcodes()).unitOfMeasure(productView.getUnitOfMeasure())
                .category(productView.getCategory()).brand(productView.getBrand()).createdAt(productView.getCreatedAt()).lastModifiedAt(productView.getLastModifiedAt()).build();
    }
}

