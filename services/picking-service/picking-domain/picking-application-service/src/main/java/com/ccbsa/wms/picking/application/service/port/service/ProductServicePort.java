package com.ccbsa.wms.picking.application.service.port.service;

import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.application.service.port.service.dto.ProductInfo;

/**
 * Service Port: ProductServicePort
 * <p>
 * Defines the contract for Product Service integration. Implemented by infrastructure adapters.
 * <p>
 * This port is used for synchronous product validation during picking list creation.
 */
public interface ProductServicePort {
    /**
     * Validates that a product exists and returns product information.
     *
     * @param productCode Product code to validate
     * @return Optional ProductInfo if product exists
     */
    Optional<ProductInfo> validateProduct(String productCode);

    /**
     * Checks if a product exists.
     *
     * @param productCode Product code to check
     * @return true if product exists
     */
    boolean productExists(String productCode);

    /**
     * Gets product information by product ID.
     *
     * @param productId Product ID (UUID as string)
     * @param tenantId  Tenant ID
     * @return Optional ProductInfo if product exists
     */
    Optional<ProductInfo> getProductById(String productId, String tenantId);

    /**
     * Gets product information by product code.
     *
     * @param productCode Product code
     * @param tenantId    Tenant ID
     * @return Optional ProductInfo if product exists
     */
    Optional<ProductInfo> getProductByCode(String productCode, TenantId tenantId);
}
