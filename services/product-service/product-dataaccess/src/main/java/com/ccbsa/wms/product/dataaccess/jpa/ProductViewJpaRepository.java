package com.ccbsa.wms.product.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ccbsa.wms.product.dataaccess.entity.ProductViewEntity;

/**
 * JPA Repository: ProductViewJpaRepository
 * <p>
 * Spring Data JPA repository for ProductViewEntity read model queries.
 * <p>
 * Provides optimized read-only queries for product views.
 */
@Repository
public interface ProductViewJpaRepository extends JpaRepository<ProductViewEntity, UUID> {

    /**
     * Finds a product view by tenant ID and product ID.
     *
     * @param tenantId  Tenant ID
     * @param productId Product ID
     * @return Optional ProductViewEntity
     */
    Optional<ProductViewEntity> findByTenantIdAndId(String tenantId, UUID productId);

    /**
     * Finds a product view by tenant ID and product code.
     *
     * @param tenantId    Tenant ID
     * @param productCode Product code
     * @return Optional ProductViewEntity
     */
    Optional<ProductViewEntity> findByTenantIdAndProductCode(String tenantId, String productCode);

    /**
     * Finds all product views for a tenant.
     *
     * @param tenantId Tenant ID
     * @return List of ProductViewEntity
     */
    List<ProductViewEntity> findByTenantId(String tenantId);

    /**
     * Checks if a product code exists for a tenant.
     *
     * @param tenantId    Tenant ID
     * @param productCode Product code
     * @return true if product code exists
     */
    boolean existsByTenantIdAndProductCode(String tenantId, String productCode);

    /**
     * Checks if a primary barcode exists for a tenant.
     *
     * @param tenantId Tenant ID
     * @param barcode  Barcode value
     * @return true if barcode exists
     */
    boolean existsByTenantIdAndPrimaryBarcode(String tenantId, String barcode);

    /**
     * Checks if a barcode exists (primary or secondary) for a tenant.
     * <p>
     * This query checks both the primary_barcode column and the product_barcodes table.
     *
     * @param tenantId Tenant ID
     * @param barcode  Barcode value
     * @return true if barcode exists
     */
    @Query("SELECT COUNT(p) > 0 FROM ProductViewEntity p WHERE p.tenantId = :tenantId AND p.primaryBarcode = :barcode "
            + "OR EXISTS (SELECT 1 FROM ProductBarcodeEntity pb WHERE pb.product.id = p.id AND pb.barcode = :barcode)")
    boolean existsByTenantIdAndBarcode(@Param("tenantId") String tenantId, @Param("barcode") String barcode);

    /**
     * Finds a product view by tenant ID and barcode (primary or secondary).
     * <p>
     * Checks primary barcode first, then secondary barcodes.
     *
     * @param tenantId Tenant ID
     * @param barcode  Barcode value
     * @return Optional ProductViewEntity
     */
    @Query("SELECT p FROM ProductViewEntity p WHERE p.tenantId = :tenantId AND p.primaryBarcode = :barcode "
            + "OR EXISTS (SELECT 1 FROM ProductBarcodeEntity pb WHERE pb.product.id = p.id AND pb.barcode = :barcode)")
    Optional<ProductViewEntity> findByTenantIdAndBarcode(@Param("tenantId") String tenantId, @Param("barcode") String barcode);

    /**
     * Finds product views with filtering and pagination support using native SQL.
     * <p>
     * Performs database-level filtering for efficient querying.
     * All filtering (category, brand, search) is done at the database level.
     *
     * @param tenantId Tenant identifier
     * @param category Optional category filter (case-insensitive, null to ignore)
     * @param brand    Optional brand filter (case-insensitive, null to ignore)
     * @param search   Optional search term (searches in product code, description, primary barcode, category, brand)
     * @param pageable Pagination information
     * @return Page of ProductViewEntity matching the criteria with pagination applied
     */
    @Query(value = "SELECT p.* FROM products p WHERE p.tenant_id = :tenantId " + "AND (:category IS NULL OR LOWER(p.category) = LOWER(:category)) "
            + "AND (:brand IS NULL OR LOWER(p.brand) = LOWER(:brand)) " + "AND (:search IS NULL OR " + "LOWER(p.product_code) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " + "LOWER(CAST(p.primary_barcode AS TEXT)) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(p.category) LIKE LOWER(CONCAT('%', :search, '%')) OR " + "LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%'))) " + "ORDER BY p.product_code ASC", countQuery =
            "SELECT COUNT(p) FROM products p WHERE p.tenant_id = :tenantId " + "AND (:category IS NULL OR LOWER(p.category) = LOWER(:category)) "
                    + "AND (:brand IS NULL OR LOWER(p.brand) = LOWER(:brand)) " + "AND (:search IS NULL OR " + "LOWER(p.product_code) LIKE LOWER(CONCAT('%', :search, '%')) OR "
                    + "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " + "LOWER(CAST(p.primary_barcode AS TEXT)) LIKE LOWER(CONCAT('%', :search, '%')) OR "
                    + "LOWER(p.category) LIKE LOWER(CONCAT('%', :search, '%')) OR " + "LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%')))", nativeQuery = true)
    org.springframework.data.domain.Page<ProductViewEntity> findByTenantIdWithFilters(@Param("tenantId") String tenantId, @Param("category") String category,
                                                                                      @Param("brand") String brand, @Param("search") String search,
                                                                                      org.springframework.data.domain.Pageable pageable);

    /**
     * Counts product views matching the filter criteria using native SQL.
     *
     * @param tenantId Tenant identifier
     * @param category Optional category filter (case-insensitive, null to ignore)
     * @param brand    Optional brand filter (case-insensitive, null to ignore)
     * @param search   Optional search term (searches in product code, description, primary barcode, category, brand)
     * @return Total count of product views matching the criteria
     */
    @Query(value = "SELECT COUNT(p) FROM products p WHERE p.tenant_id = :tenantId " + "AND (:category IS NULL OR LOWER(p.category) = LOWER(:category)) "
            + "AND (:brand IS NULL OR LOWER(p.brand) = LOWER(:brand)) " + "AND (:search IS NULL OR " + "LOWER(p.product_code) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " + "LOWER(CAST(p.primary_barcode AS TEXT)) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(p.category) LIKE LOWER(CONCAT('%', :search, '%')) OR " + "LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%')))", nativeQuery = true)
    long countByTenantIdWithFilters(@Param("tenantId") String tenantId, @Param("category") String category, @Param("brand") String brand, @Param("search") String search);
}

