package com.ccbsa.wms.product.dataaccess.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.application.service.port.repository.ProductRepository;
import com.ccbsa.wms.product.dataaccess.entity.ProductBarcodeEntity;
import com.ccbsa.wms.product.dataaccess.entity.ProductEntity;
import com.ccbsa.wms.product.dataaccess.jpa.ProductBarcodeJpaRepository;
import com.ccbsa.wms.product.dataaccess.jpa.ProductJpaRepository;
import com.ccbsa.wms.product.dataaccess.mapper.ProductEntityMapper;
import com.ccbsa.wms.product.domain.core.entity.Product;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductId;

/**
 * Repository Adapter: ProductRepositoryAdapter
 * <p>
 * Implements ProductRepository port interface. Adapts between domain Product aggregate and JPA ProductEntity.
 */
@Repository
public class ProductRepositoryAdapter
        implements ProductRepository {
    private final ProductJpaRepository jpaRepository;
    private final ProductBarcodeJpaRepository barcodeJpaRepository;
    private final ProductEntityMapper mapper;

    public ProductRepositoryAdapter(ProductJpaRepository jpaRepository, ProductBarcodeJpaRepository barcodeJpaRepository, ProductEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.barcodeJpaRepository = barcodeJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Product save(Product product) {
        // Check if entity already exists to handle version correctly
        Optional<ProductEntity> existingEntity = jpaRepository.findByTenantIdAndId(product.getTenantId()
                .getValue(), product.getId()
                .getValue());

        ProductEntity entity;
        if (existingEntity.isPresent()) {
            // Update existing entity - preserve JPA managed state and version
            entity = existingEntity.get();
            updateEntityFromDomain(entity, product);
        } else {
            // New entity - create from domain model
            entity = mapper.toEntity(product);
        }

        ProductEntity savedEntity = jpaRepository.save(entity);
        Product savedProduct = mapper.toDomain(savedEntity);

        // Domain events are preserved by the command handler before calling save()
        // The command handler gets domain events from the original product before save()
        // and publishes them after transaction commit. We return the saved product
        // which may not have events, but that's OK since events are already captured.
        return savedProduct;
    }

    /**
     * Updates an existing entity with values from the domain model. Preserves JPA managed state and version for optimistic locking.
     *
     * @param entity  Existing JPA entity
     * @param product Domain product aggregate
     */
    private void updateEntityFromDomain(ProductEntity entity, Product product) {
        entity.setProductCode(product.getProductCode()
                .getValue());
        entity.setDescription(product.getDescription());
        entity.setPrimaryBarcode(product.getPrimaryBarcode()
                .getValue());
        entity.setPrimaryBarcodeType(product.getPrimaryBarcode()
                .getType());
        entity.setUnitOfMeasure(product.getUnitOfMeasure());
        entity.setCategory(product.getCategory());
        entity.setBrand(product.getBrand());
        entity.setLastModifiedAt(product.getLastModifiedAt());

        // Update secondary barcodes - remove all existing and add new ones
        entity.getSecondaryBarcodes()
                .clear();
        for (ProductBarcode barcode : product.getSecondaryBarcodes()) {
            ProductBarcodeEntity barcodeEntity = new ProductBarcodeEntity();
            barcodeEntity.setId(UUID.randomUUID());
            barcodeEntity.setProduct(entity);
            barcodeEntity.setBarcode(barcode.getValue());
            barcodeEntity.setBarcodeType(barcode.getType());
            entity.getSecondaryBarcodes()
                    .add(barcodeEntity);
        }

        // Version is managed by JPA - don't update it manually
    }

    @Override
    public Optional<Product> findByIdAndTenantId(ProductId id, TenantId tenantId) {
        return jpaRepository.findByTenantIdAndId(tenantId.getValue(), id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Product> findByProductCodeAndTenantId(ProductCode productCode, TenantId tenantId) {
        return jpaRepository.findByTenantIdAndProductCode(tenantId.getValue(), productCode.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByProductCodeAndTenantId(ProductCode productCode, TenantId tenantId) {
        return jpaRepository.existsByTenantIdAndProductCode(tenantId.getValue(), productCode.getValue());
    }

    @Override
    public boolean existsByBarcodeAndTenantId(ProductBarcode barcode, TenantId tenantId) {
        // Check primary barcode
        if (jpaRepository.existsByTenantIdAndPrimaryBarcode(tenantId.getValue(), barcode.getValue())) {
            return true;
        }
        // Check secondary barcodes
        return barcodeJpaRepository.existsByBarcode(barcode.getValue());
    }

    @Override
    public Optional<Product> findByBarcodeAndTenantId(String barcode, TenantId tenantId) {
        // First check primary barcode
        Optional<ProductEntity> primaryBarcodeProduct = jpaRepository.findByTenantIdAndPrimaryBarcode(tenantId.getValue(), barcode);
        if (primaryBarcodeProduct.isPresent()) {
            return primaryBarcodeProduct.map(mapper::toDomain);
        }

        // Then check secondary barcodes
        Optional<ProductBarcodeEntity> secondaryBarcode = barcodeJpaRepository.findByBarcodeAndTenantId(barcode, tenantId.getValue());
        if (secondaryBarcode.isPresent()) {
            ProductEntity productEntity = secondaryBarcode.get()
                    .getProduct();
            return Optional.of(mapper.toDomain(productEntity));
        }

        return Optional.empty();
    }

    @Override
    public List<Product> findByTenantId(TenantId tenantId) {
        return jpaRepository.findByTenantId(tenantId.getValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findByTenantIdAndCategory(TenantId tenantId, String category) {
        return jpaRepository.findByTenantIdAndCategory(tenantId.getValue(), category)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}

