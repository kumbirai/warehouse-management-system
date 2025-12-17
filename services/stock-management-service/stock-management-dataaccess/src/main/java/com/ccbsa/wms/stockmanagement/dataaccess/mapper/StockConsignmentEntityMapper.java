package com.ccbsa.wms.stockmanagement.dataaccess.mapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.WarehouseId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.stockmanagement.dataaccess.entity.ConsignmentLineItemEntity;
import com.ccbsa.wms.stockmanagement.dataaccess.entity.StockConsignmentEntity;
import com.ccbsa.wms.stockmanagement.domain.core.entity.StockConsignment;
import com.ccbsa.wms.stockmanagement.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stockmanagement.domain.core.valueobject.ConsignmentLineItem;
import com.ccbsa.wms.stockmanagement.domain.core.valueobject.ConsignmentReference;

/**
 * Mapper: StockConsignmentEntityMapper
 * <p>
 * Maps between StockConsignment domain aggregate and StockConsignmentEntity JPA entity. Handles conversion between domain value objects and JPA entity fields.
 */
@Component
public class StockConsignmentEntityMapper {

    /**
     * Converts StockConsignment domain entity to StockConsignmentEntity JPA entity.
     *
     * @param consignment StockConsignment domain entity
     * @return StockConsignmentEntity JPA entity
     * @throws IllegalArgumentException if consignment is null
     */
    public StockConsignmentEntity toEntity(StockConsignment consignment) {
        if (consignment == null) {
            throw new IllegalArgumentException("StockConsignment cannot be null");
        }

        StockConsignmentEntity entity = new StockConsignmentEntity();
        entity.setId(consignment.getId()
                .getValue());
        entity.setTenantId(consignment.getTenantId()
                .getValue());
        entity.setConsignmentReference(consignment.getConsignmentReference()
                .getValue());
        entity.setWarehouseId(consignment.getWarehouseId()
                .getValue());
        entity.setStatus(consignment.getStatus());
        entity.setReceivedAt(consignment.getReceivedAt());
        entity.setConfirmedAt(consignment.getConfirmedAt());
        entity.setReceivedBy(consignment.getReceivedBy());
        entity.setCreatedAt(consignment.getCreatedAt());
        entity.setLastModifiedAt(consignment.getLastModifiedAt());

        // Map line items
        List<ConsignmentLineItemEntity> lineItemEntities = new ArrayList<>();
        for (ConsignmentLineItem lineItem : consignment.getLineItems()) {
            ConsignmentLineItemEntity lineItemEntity = new ConsignmentLineItemEntity();
            lineItemEntity.setId(UUID.randomUUID());
            lineItemEntity.setConsignment(entity);
            lineItemEntity.setProductCode(lineItem.getProductCode()
                    .getValue());
            lineItemEntity.setQuantity(lineItem.getQuantity());
            lineItemEntity.setExpirationDate(lineItem.getExpirationDate());
            lineItemEntity.setCreatedAt(LocalDateTime.now());
            lineItemEntities.add(lineItemEntity);
        }
        entity.setLineItems(lineItemEntities);

        // Set version for optimistic locking
        int domainVersion = consignment.getVersion();
        if (domainVersion > 0) {
            entity.setVersion(Long.valueOf(domainVersion));
        }

        return entity;
    }

    /**
     * Converts StockConsignmentEntity JPA entity to StockConsignment domain entity.
     *
     * @param entity StockConsignmentEntity JPA entity
     * @return StockConsignment domain entity
     * @throws IllegalArgumentException if entity is null
     */
    public StockConsignment toDomain(StockConsignmentEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("StockConsignmentEntity cannot be null");
        }

        // Convert line items
        List<ConsignmentLineItem> lineItems = new ArrayList<>();
        if (entity.getLineItems() != null) {
            for (ConsignmentLineItemEntity lineItemEntity : entity.getLineItems()) {
                ConsignmentLineItem lineItem = ConsignmentLineItem.builder()
                        .productCode(ProductCode.of(lineItemEntity.getProductCode()))
                        .quantity(lineItemEntity.getQuantity())
                        .expirationDate(lineItemEntity.getExpirationDate())
                        .build();
                lineItems.add(lineItem);
            }
        }

        // Build domain entity using builder
        StockConsignment consignment = StockConsignment.builder()
                .consignmentId(ConsignmentId.of(entity.getId()))
                .tenantId(TenantId.of(entity.getTenantId()))
                .consignmentReference(ConsignmentReference.of(entity.getConsignmentReference()))
                .warehouseId(WarehouseId.of(entity.getWarehouseId()))
                .receivedAt(entity.getReceivedAt())
                .receivedBy(entity.getReceivedBy())
                .lineItems(lineItems)
                .status(entity.getStatus())
                .confirmedAt(entity.getConfirmedAt())
                .createdAt(entity.getCreatedAt())
                .lastModifiedAt(entity.getLastModifiedAt())
                .version(entity.getVersion() != null ? entity.getVersion()
                        .intValue() : 0)
                .buildWithoutEvents();

        return consignment;
    }
}

