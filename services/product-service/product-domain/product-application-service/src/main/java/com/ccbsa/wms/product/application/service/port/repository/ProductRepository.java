package com.ccbsa.wms.product.application.service.port.repository;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.domain.core.entity.Product;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductId;

/**
 * Repository Port: ProductRepository
 * <p>
 * Defines the contract for Product aggregate persistence.
 * Implemented by data access adapters.
 * <p>
 * This port is defined in the application service layer (not domain core)
 * to maintain proper dependency direction in hexagonal architecture.
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
     * Finds a Product by barcode and tenant ID.
     * Searches both primary and secondary barcodes.
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
}

