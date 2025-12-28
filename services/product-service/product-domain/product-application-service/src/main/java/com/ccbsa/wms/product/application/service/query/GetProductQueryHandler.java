package com.ccbsa.wms.product.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.product.application.service.port.data.ProductViewRepository;
import com.ccbsa.wms.product.application.service.query.dto.GetProductQuery;
import com.ccbsa.wms.product.application.service.query.dto.ProductQueryResult;
import com.ccbsa.wms.product.domain.core.exception.ProductNotFoundException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;

/**
 * Query Handler: GetProductQueryHandler
 * <p>
 * Handles retrieval of Product read model by ID.
 * <p>
 * Responsibilities:
 * - Load Product view from data port (read model)
 * - Map view to query result DTO
 * - Return optimized read model
 * <p>
 * Uses data port (ProductViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@RequiredArgsConstructor
@SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE", justification = "productView is used in builder chain - SpotBugs false positive with var keyword")
public class GetProductQueryHandler {
    private final ProductViewRepository viewRepository;

    @Transactional(readOnly = true)
    public ProductQueryResult handle(GetProductQuery query) {
        // 1. Load read model (view) from data port
        var productView = viewRepository.findByTenantIdAndId(query.getTenantId(), query.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(String.format("Product not found: %s", query.getProductId().getValueAsString())));

        // 2. Map view to query result
        return ProductQueryResult.builder().productId(productView.getProductId()).productCode(productView.getProductCode()).description(productView.getDescription())
                .primaryBarcode(productView.getPrimaryBarcode()).secondaryBarcodes(productView.getSecondaryBarcodes()).unitOfMeasure(productView.getUnitOfMeasure())
                .category(productView.getCategory()).brand(productView.getBrand()).createdAt(productView.getCreatedAt()).lastModifiedAt(productView.getLastModifiedAt()).build();
    }
}

