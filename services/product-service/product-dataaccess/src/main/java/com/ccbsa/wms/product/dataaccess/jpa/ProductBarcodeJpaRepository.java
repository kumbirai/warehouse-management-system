package com.ccbsa.wms.product.dataaccess.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ccbsa.wms.product.dataaccess.entity.ProductBarcodeEntity;

/**
 * JPA Repository: ProductBarcodeJpaRepository
 * <p>
 * Spring Data JPA repository for ProductBarcodeEntity. Provides database access methods for secondary barcodes.
 */
public interface ProductBarcodeJpaRepository extends JpaRepository<ProductBarcodeEntity, UUID> {
    /**
     * Finds a barcode entity by barcode value.
     *
     * @param barcode Barcode value
     * @return Optional ProductBarcodeEntity if found
     */
    Optional<ProductBarcodeEntity> findByBarcode(String barcode);

    /**
     * Checks if a barcode exists.
     *
     * @param barcode Barcode value
     * @return true if barcode exists
     */
    boolean existsByBarcode(String barcode);

    /**
     * Finds a barcode entity by barcode value and tenant ID. Joins with ProductEntity to filter by tenant.
     *
     * @param barcode  Barcode value
     * @param tenantId Tenant identifier
     * @return Optional ProductBarcodeEntity if found
     */
    @Query("SELECT pb FROM ProductBarcodeEntity pb " + "JOIN pb.product p " + "WHERE pb.barcode = :barcode AND p.tenantId = :tenantId")
    Optional<ProductBarcodeEntity> findByBarcodeAndTenantId(@Param("barcode") String barcode, @Param("tenantId") String tenantId);
}

