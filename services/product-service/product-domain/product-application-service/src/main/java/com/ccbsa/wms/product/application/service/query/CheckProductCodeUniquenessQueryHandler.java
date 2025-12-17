package com.ccbsa.wms.product.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.product.application.service.port.repository.ProductRepository;
import com.ccbsa.wms.product.application.service.query.dto.CheckProductCodeUniquenessQuery;
import com.ccbsa.wms.product.application.service.query.dto.ProductCodeUniquenessResult;

/**
 * Query Handler: CheckProductCodeUniquenessQueryHandler
 * <p>
 * Handles checking if a product code is unique for a tenant.
 * <p>
 * Responsibilities: - Check product code uniqueness in repository - Return uniqueness result
 */
@Component
public class CheckProductCodeUniquenessQueryHandler {

    private final ProductRepository repository;

    public CheckProductCodeUniquenessQueryHandler(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ProductCodeUniquenessResult handle(CheckProductCodeUniquenessQuery query) {
        // Check if product code exists
        boolean isUnique = !repository.existsByProductCodeAndTenantId(query.getProductCode(), query.getTenantId());

        return ProductCodeUniquenessResult.builder()
                .productCode(query.getProductCode())
                .isUnique(isUnique)
                .build();
    }
}

