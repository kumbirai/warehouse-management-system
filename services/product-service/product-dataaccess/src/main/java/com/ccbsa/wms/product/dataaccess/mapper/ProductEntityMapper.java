package com.ccbsa.wms.product.dataaccess.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.dataaccess.entity.ProductBarcodeEntity;
import com.ccbsa.wms.product.dataaccess.entity.ProductEntity;
import com.ccbsa.wms.product.domain.core.entity.Product;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductId;

/**
 * Mapper: ProductEntityMapper
 * <p>
 * Maps between Product domain aggregate and ProductEntity JPA entity.
 * Handles conversion between domain value objects and JPA entity fields.
 */
@Component
public class ProductEntityMapper {

    /**
     * Converts Product domain entity to ProductEntity JPA entity.
     * <p>
     * For new entities (version == 0), version is set to null to let Hibernate manage it.
     * For existing entities (version > 0), version is set to enable optimistic locking.
     *
     * @param product Product domain entity
     * @return ProductEntity JPA entity
     * @throws IllegalArgumentException if product is null
     */
    public ProductEntity toEntity(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }

        ProductEntity entity = new ProductEntity();
        entity.setId(product.getId().getValue());
        entity.setTenantId(product.getTenantId().getValue());
        entity.setProductCode(product.getProductCode().getValue());
        entity.setDescription(product.getDescription());
        entity.setPrimaryBarcode(product.getPrimaryBarcode().getValue());
        entity.setPrimaryBarcodeType(product.getPrimaryBarcode().getType());
        entity.setUnitOfMeasure(product.getUnitOfMeasure());
        entity.setCategory(product.getCategory());
        entity.setBrand(product.getBrand());
        entity.setCreatedAt(product.getCreatedAt());
        entity.setLastModifiedAt(product.getLastModifiedAt());

        // Map secondary barcodes
        List<ProductBarcodeEntity> secondaryBarcodeEntities = new ArrayList<>();
        for (ProductBarcode barcode : product.getSecondaryBarcodes()) {
            ProductBarcodeEntity barcodeEntity = new ProductBarcodeEntity();
            barcodeEntity.setId(UUID.randomUUID());
            barcodeEntity.setProduct(entity);
            barcodeEntity.setBarcode(barcode.getValue());
            barcodeEntity.setBarcodeType(barcode.getType());
            secondaryBarcodeEntities.add(barcodeEntity);
        }
        entity.setSecondaryBarcodes(secondaryBarcodeEntities);

        // For new entities, version will be set by Hibernate when persisting
        // For existing entities loaded from DB, version is already set
        // We only set version when mapping from domain if it's > 0 (existing entity)
        int domainVersion = product.getVersion();
        if (domainVersion > 0) {
            entity.setVersion(Long.valueOf(domainVersion));
        }
        // For new entities (version == 0), don't set version - let Hibernate manage it

        return entity;
    }

    /**
     * Converts ProductEntity JPA entity to Product domain entity.
     *
     * @param entity ProductEntity JPA entity
     * @return Product domain entity
     * @throws IllegalArgumentException if entity is null
     */
    public Product toDomain(ProductEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("ProductEntity cannot be null");
        }

        Product.Builder builder = Product.builder()
                .productId(ProductId.of(entity.getId()))
                .tenantId(TenantId.of(entity.getTenantId()))
                .productCode(ProductCode.of(entity.getProductCode()))
                .description(entity.getDescription())
                .primaryBarcode(ProductBarcode.of(entity.getPrimaryBarcode(), entity.getPrimaryBarcodeType()))
                .unitOfMeasure(entity.getUnitOfMeasure())
                .createdAt(entity.getCreatedAt())
                .lastModifiedAt(entity.getLastModifiedAt())
                .version(entity.getVersion());

        // Map secondary barcodes
        if (entity.getSecondaryBarcodes() != null && !entity.getSecondaryBarcodes().isEmpty()) {
            List<ProductBarcode> secondaryBarcodes = new ArrayList<>();
            for (ProductBarcodeEntity barcodeEntity : entity.getSecondaryBarcodes()) {
                ProductBarcode barcode = ProductBarcode.of(
                        barcodeEntity.getBarcode(),
                        barcodeEntity.getBarcodeType()
                );
                secondaryBarcodes.add(barcode);
            }
            builder.secondaryBarcodes(secondaryBarcodes);
        }

        // Set optional fields
        if (entity.getCategory() != null && !entity.getCategory().trim().isEmpty()) {
            builder.category(entity.getCategory());
        }
        if (entity.getBrand() != null && !entity.getBrand().trim().isEmpty()) {
            builder.brand(entity.getBrand());
        }

        return builder.buildWithoutEvents();
    }
}

