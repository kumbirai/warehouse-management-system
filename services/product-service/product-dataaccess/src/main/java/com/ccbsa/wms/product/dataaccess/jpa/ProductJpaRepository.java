package com.ccbsa.wms.product.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ccbsa.wms.product.dataaccess.entity.ProductEntity;

/**
 * JPA Repository: ProductJpaRepository
 * <p>
 * Spring Data JPA repository for ProductEntity. Provides database access methods with multi-tenant support.
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
     * Checks if a product exists with the given primary barcode for the tenant, excluding a specific product.
     *
     * @param tenantId  Tenant identifier
     * @param barcode   Primary barcode
     * @param excludeId Product ID to exclude from the check
     * @return true if product exists (excluding the specified product)
     */
    boolean existsByTenantIdAndPrimaryBarcodeAndIdNot(String tenantId, String barcode, UUID excludeId);

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

    /**
     * Finds products with filtering and pagination support using native SQL.
     * <p>
     * Performs database-level filtering for efficient querying.
     * All filtering (category, brand, search) is done at the database level.
     * <p>
     * Uses native SQL to handle bytea primary_barcode column by casting to text before using LOWER().
     * <p>
     * Returns Page to enable automatic pagination by Spring Data JPA.
     *
     * @param tenantId Tenant identifier
     * @param category Optional category filter (case-insensitive, null to ignore)
     * @param brand    Optional brand filter (case-insensitive, null to ignore)
     * @param search   Optional search term (searches in product code, description, primary barcode, category, brand)
     * @param pageable Pagination information
     * @return Page of ProductEntity matching the criteria with pagination applied
     */
    @Query(value = "SELECT p.* FROM products p WHERE p.tenant_id = :tenantId " + "AND (:category IS NULL OR LOWER(p.category) = LOWER(:category)) "
            + "AND (:brand IS NULL OR LOWER(p.brand) = LOWER(:brand)) " + "AND (:search IS NULL OR " + "LOWER(p.product_code) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " + "LOWER(CAST(p.primary_barcode AS TEXT)) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(p.category) LIKE LOWER(CONCAT('%', :search, '%')) OR " + "LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%'))) " + "ORDER BY p.product_code ASC", countQuery =
            "SELECT COUNT(p) FROM products p WHERE p.tenant_id = :tenantId " + "AND (:category IS NULL OR LOWER(p.category) = LOWER(:category)) "
                    + "AND (:brand IS NULL OR LOWER(p.brand) = LOWER(:brand)) " + "AND (:search IS NULL OR " + "LOWER(p.product_code) LIKE LOWER(CONCAT('%', :search, '%')) OR "
                    + "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " + "LOWER(CAST(p.primary_barcode AS TEXT)) LIKE LOWER(CONCAT('%', :search, '%')) OR "
                    + "LOWER(p.category) LIKE LOWER(CONCAT('%', :search, '%')) OR " + "LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%')))", nativeQuery = true)
    Page<ProductEntity> findByTenantIdWithFilters(@Param("tenantId") String tenantId, @Param("category") String category, @Param("brand") String brand,
                                                  @Param("search") String search, Pageable pageable);

    /**
     * Counts products matching the filter criteria using native SQL.
     * <p>
     * Used for pagination to determine total count of matching products.
     * <p>
     * Uses native SQL to handle bytea primary_barcode column by casting to text before using LOWER().
     *
     * @param tenantId Tenant identifier
     * @param category Optional category filter (case-insensitive, null to ignore)
     * @param brand    Optional brand filter (case-insensitive, null to ignore)
     * @param search   Optional search term (searches in product code, description, primary barcode, category, brand)
     * @return Total count of products matching the criteria
     */
    @Query(value = "SELECT COUNT(p) FROM products p WHERE p.tenant_id = :tenantId " + "AND (:category IS NULL OR LOWER(p.category) = LOWER(:category)) "
            + "AND (:brand IS NULL OR LOWER(p.brand) = LOWER(:brand)) " + "AND (:search IS NULL OR " + "LOWER(p.product_code) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " + "LOWER(CAST(p.primary_barcode AS TEXT)) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(p.category) LIKE LOWER(CONCAT('%', :search, '%')) OR " + "LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%')))", nativeQuery = true)
    long countByTenantIdWithFilters(@Param("tenantId") String tenantId, @Param("category") String category, @Param("brand") String brand, @Param("search") String search);
}

