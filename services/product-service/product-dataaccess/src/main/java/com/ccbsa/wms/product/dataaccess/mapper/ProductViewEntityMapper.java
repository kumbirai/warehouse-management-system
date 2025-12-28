package com.ccbsa.wms.product.dataaccess.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.wms.product.application.service.port.data.dto.ProductView;
import com.ccbsa.wms.product.dataaccess.entity.ProductBarcodeEntity;
import com.ccbsa.wms.product.dataaccess.entity.ProductViewEntity;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;

/**
 * Entity Mapper: ProductViewEntityMapper
 * <p>
 * Maps between ProductViewEntity (JPA) and ProductView (read model DTO).
 */
@Component
public class ProductViewEntityMapper {

    /**
     * Converts ProductViewEntity JPA entity to ProductView read model DTO.
     * <p>
     * Convenience method when secondary barcodes are not needed.
     *
     * @param entity ProductViewEntity JPA entity
     * @return ProductView read model DTO
     */
    public ProductView toView(ProductViewEntity entity) {
        return toView(entity, null);
    }

    /**
     * Converts ProductViewEntity JPA entity to ProductView read model DTO.
     *
     * @param entity          ProductViewEntity JPA entity
     * @param barcodeEntities Secondary barcode entities (optional)
     * @return ProductView read model DTO
     * @throws IllegalArgumentException if entity is null
     */
    public ProductView toView(ProductViewEntity entity, List<ProductBarcodeEntity> barcodeEntities) {
        if (entity == null) {
            throw new IllegalArgumentException("ProductViewEntity cannot be null");
        }

        // Map secondary barcodes
        List<ProductBarcode> secondaryBarcodes = new ArrayList<>();
        if (barcodeEntities != null && !barcodeEntities.isEmpty()) {
            secondaryBarcodes = barcodeEntities.stream().map(be -> ProductBarcode.of(be.getBarcode(), be.getBarcodeType())).collect(Collectors.toList());
        }

        // Build ProductView
        return ProductView.builder().productId(ProductId.of(entity.getId())).tenantId(entity.getTenantId()).productCode(ProductCode.of(entity.getProductCode()))
                .description(entity.getDescription()).primaryBarcode(ProductBarcode.of(entity.getPrimaryBarcode(), entity.getPrimaryBarcodeType()))
                .secondaryBarcodes(secondaryBarcodes).unitOfMeasure(entity.getUnitOfMeasure()).category(entity.getCategory()).brand(entity.getBrand())
                .createdAt(entity.getCreatedAt()).lastModifiedAt(entity.getLastModifiedAt()).build();
    }
}

