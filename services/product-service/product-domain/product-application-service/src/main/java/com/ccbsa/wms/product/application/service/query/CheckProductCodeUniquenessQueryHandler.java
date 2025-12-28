package com.ccbsa.wms.product.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.product.application.service.port.data.ProductViewRepository;
import com.ccbsa.wms.product.application.service.query.dto.CheckProductCodeUniquenessQuery;
import com.ccbsa.wms.product.application.service.query.dto.ProductCodeUniquenessResult;

import lombok.RequiredArgsConstructor;

/**
 * Query Handler: CheckProductCodeUniquenessQueryHandler
 * <p>
 * Handles checking if a product code is unique for a tenant.
 * <p>
 * Responsibilities:
 * - Check product code uniqueness in data port (read model)
 * - Return uniqueness result
 * <p>
 * Uses data port (ProductViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@RequiredArgsConstructor
public class CheckProductCodeUniquenessQueryHandler {
    private final ProductViewRepository viewRepository;

    @Transactional(readOnly = true)
    public ProductCodeUniquenessResult handle(CheckProductCodeUniquenessQuery query) {
        // Check if product code exists in read model
        boolean isUnique = !viewRepository.existsByTenantIdAndProductCode(query.getTenantId(), query.getProductCode().getValue());

        return ProductCodeUniquenessResult.builder().productCode(query.getProductCode()).isUnique(isUnique).build();
    }
}

