package com.ccbsa.wms.product.application.service.port.repository;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.domain.core.entity.Product;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;

/**
 * Repository Port: ProductRepository
 * <p>
 * Defines the contract for Product aggregate persistence. Implemented by data access adapters.
 * <p>
 * This port is defined in the application service layer (not domain core) to maintain proper dependency direction in hexagonal architecture.
 */
public interface ProductRepository {
    /**
     * Saves a Product aggregate.
     * <p>
     * Creates a new product if it doesn't exist, or updates an existing one.
     *
     * @param product Product aggregate to save
     * @return Saved Product aggregate (may be a new instance from mapper)
     */
    Product save(Product product);

    /**
     * Finds a Product by ID and tenant ID.
     *
     * @param id       Product identifier
     * @param tenantId Tenant identifier
     * @return Optional Product if found
     */
    Optional<Product> findByIdAndTenantId(ProductId id, TenantId tenantId);

    /**
     * Finds a Product by product code and tenant ID.
     *
     * @param productCode Product code
     * @param tenantId    Tenant identifier
     * @return Optional Product if found
     */
    Optional<Product> findByProductCodeAndTenantId(ProductCode productCode, TenantId tenantId);

    /**
     * Checks if a product with the given product code exists for the tenant.
     *
     * @param productCode Product code
     * @param tenantId    Tenant identifier
     * @return true if product exists with the product code
     */
    boolean existsByProductCodeAndTenantId(ProductCode productCode, TenantId tenantId);

    /**
     * Checks if a product with the given barcode exists for the tenant.
     *
     * @param barcode  Product barcode
     * @param tenantId Tenant identifier
     * @return true if product exists with the barcode
     */
    boolean existsByBarcodeAndTenantId(ProductBarcode barcode, TenantId tenantId);

    /**
     * Checks if a product with the given barcode exists for the tenant, excluding a specific product.
     * <p>
     * Used when updating a product to allow the same barcode if it belongs to the product being updated.
     *
     * @param barcode          Product barcode
     * @param tenantId         Tenant identifier
     * @param excludeProductId Product ID to exclude from the check
     * @return true if product exists with the barcode (excluding the specified product)
     */
    boolean existsByBarcodeAndTenantIdExcludingProduct(ProductBarcode barcode, TenantId tenantId, ProductId excludeProductId);

    /**
     * Finds a Product by barcode and tenant ID. Searches both primary and secondary barcodes.
     *
     * @param barcode  Barcode value (as string)
     * @param tenantId Tenant identifier
     * @return Optional Product if found
     */
    Optional<Product> findByBarcodeAndTenantId(String barcode, TenantId tenantId);

    /**
     * Finds all products for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return List of products for the tenant
     */
    List<Product> findByTenantId(TenantId tenantId);

    /**
     * Finds products by tenant ID and category.
     *
     * @param tenantId Tenant identifier
     * @param category Product category
     * @return List of products matching the criteria
     */
    List<Product> findByTenantIdAndCategory(TenantId tenantId, String category);

    /**
     * Finds products with filtering and pagination support.
     * <p>
     * This method performs database-level filtering for efficient querying.
     * All filtering (category, brand, search) is done at the database level.
     *
     * @param tenantId Tenant identifier
     * @param category Optional category filter (case-insensitive)
     * @param brand    Optional brand filter (case-insensitive)
     * @param search   Optional search term (searches in product code, description, primary barcode, category, brand)
     * @param page     Page number (0-based)
     * @param size     Page size
     * @return List of products matching the criteria with pagination applied
     */
    List<Product> findByTenantIdWithFilters(TenantId tenantId, String category, String brand, String search, int page, int size);

    /**
     * Counts products matching the filter criteria.
     * <p>
     * This method is used for pagination to determine total count of matching products.
     *
     * @param tenantId Tenant identifier
     * @param category Optional category filter (case-insensitive)
     * @param brand    Optional brand filter (case-insensitive)
     * @param search   Optional search term (searches in product code, description, primary barcode, category, brand)
     * @return Total count of products matching the criteria
     */
    long countByTenantIdWithFilters(TenantId tenantId, String category, String brand, String search);
}

