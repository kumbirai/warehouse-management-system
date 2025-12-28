package com.ccbsa.wms.product.application.service.port.data;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.application.service.port.data.dto.ProductView;

/**
 * Data Port: ProductViewRepository
 * <p>
 * Read model repository for product queries. Provides optimized read access to product data.
 * <p>
 * This is a data port (read model) used by query handlers, not a repository port (write model).
 * <p>
 * Responsibilities:
 * - Provide optimized read model queries
 * - Support eventual consistency (read model may lag behind write model)
 * - Enable query performance optimization through denormalization
 */
public interface ProductViewRepository {

    /**
     * Finds a product view by tenant ID and product ID.
     *
     * @param tenantId  Tenant ID
     * @param productId Product ID
     * @return Optional ProductView
     */
    Optional<ProductView> findByTenantIdAndId(TenantId tenantId, ProductId productId);

    /**
     * Finds a product view by tenant ID and product code.
     *
     * @param tenantId    Tenant ID
     * @param productCode Product code
     * @return Optional ProductView
     */
    Optional<ProductView> findByTenantIdAndProductCode(TenantId tenantId, String productCode);

    /**
     * Finds all product views for a tenant.
     *
     * @param tenantId Tenant ID
     * @return List of ProductView
     */
    List<ProductView> findByTenantId(TenantId tenantId);

    /**
     * Checks if a product code exists for a tenant.
     *
     * @param tenantId    Tenant ID
     * @param productCode Product code
     * @return true if product code exists
     */
    boolean existsByTenantIdAndProductCode(TenantId tenantId, String productCode);

    /**
     * Checks if a barcode exists for a tenant (primary or secondary).
     *
     * @param tenantId Tenant ID
     * @param barcode  Barcode value
     * @return true if barcode exists
     */
    boolean existsByTenantIdAndBarcode(TenantId tenantId, String barcode);

    /**
     * Finds a product view by tenant ID and barcode (primary or secondary).
     *
     * @param tenantId Tenant ID
     * @param barcode  Barcode value
     * @return Optional ProductView
     */
    Optional<ProductView> findByTenantIdAndBarcode(TenantId tenantId, String barcode);

    /**
     * Finds product views for a tenant with optional filters.
     *
     * @param tenantId Tenant ID
     * @param category Optional category filter
     * @param brand    Optional brand filter
     * @param search   Optional search term (searches in product code and description)
     * @param page     Page number (0-based)
     * @param size     Page size
     * @return List of ProductView
     */
    List<ProductView> findByTenantIdWithFilters(TenantId tenantId, String category, String brand, String search, int page, int size);

    /**
     * Counts product views for a tenant with optional filters.
     *
     * @param tenantId Tenant ID
     * @param category Optional category filter
     * @param brand    Optional brand filter
     * @param search   Optional search term (searches in product code and description)
     * @return Total count
     */
    long countByTenantIdWithFilters(TenantId tenantId, String category, String brand, String search);
}

