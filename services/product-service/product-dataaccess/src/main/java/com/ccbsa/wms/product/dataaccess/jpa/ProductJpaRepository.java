package com.ccbsa.wms.product.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ccbsa.wms.product.dataaccess.entity.ProductEntity;

/**
 * JPA Repository: ProductJpaRepository
 * <p>
 * Spring Data JPA repository for ProductEntity.
 * Provides database access methods with multi-tenant support.
 */
public interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID> {
    /**
     * Finds a product by tenant ID and product ID.
     *
     * @param tenantId Tenant identifier
     * @param id       Product identifier
     * @return Optional ProductEntity if found
     */
    Optional<ProductEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Finds a product by tenant ID and product code.
     *
     * @param tenantId    Tenant identifier
     * @param productCode Product code
     * @return Optional ProductEntity if found
     */
    Optional<ProductEntity> findByTenantIdAndProductCode(String tenantId, String productCode);

    /**
     * Checks if a product exists with the given product code for the tenant.
     *
     * @param tenantId    Tenant identifier
     * @param productCode Product code
     * @return true if product exists
     */
    boolean existsByTenantIdAndProductCode(String tenantId, String productCode);

    /**
     * Checks if a product exists with the given primary barcode for the tenant.
     *
     * @param tenantId Tenant identifier
     * @param barcode  Primary barcode
     * @return true if product exists
     */
    boolean existsByTenantIdAndPrimaryBarcode(String tenantId, String barcode);

    /**
     * Finds a product by tenant ID and primary barcode.
     *
     * @param tenantId Tenant identifier
     * @param barcode  Primary barcode
     * @return Optional ProductEntity if found
     */
    Optional<ProductEntity> findByTenantIdAndPrimaryBarcode(String tenantId, String barcode);

    /**
     * Finds all products for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return List of ProductEntity for the tenant
     */
    List<ProductEntity> findByTenantId(String tenantId);

    /**
     * Finds products by tenant ID and category.
     *
     * @param tenantId Tenant identifier
     * @param category Product category
     * @return List of ProductEntity matching the criteria
     */
    List<ProductEntity> findByTenantIdAndCategory(String tenantId, String category);
}

