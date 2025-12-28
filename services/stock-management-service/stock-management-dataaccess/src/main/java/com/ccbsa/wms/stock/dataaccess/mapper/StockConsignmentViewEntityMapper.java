package com.ccbsa.wms.stock.dataaccess.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.WarehouseId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockConsignmentView;
import com.ccbsa.wms.stock.dataaccess.entity.ConsignmentLineItemEntity;
import com.ccbsa.wms.stock.dataaccess.entity.StockConsignmentViewEntity;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentLineItem;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentReference;
import com.ccbsa.wms.stock.domain.core.valueobject.ReceivedBy;

/**
 * Entity Mapper: StockConsignmentViewEntityMapper
 * <p>
 * Maps between StockConsignmentViewEntity (JPA) and StockConsignmentView (read model DTO).
 */
@Component
public class StockConsignmentViewEntityMapper {

    /**
     * Converts StockConsignmentViewEntity JPA entity to StockConsignmentView read model DTO.
     *
     * @param entity StockConsignmentViewEntity JPA entity
     * @return StockConsignmentView read model DTO
     * @throws IllegalArgumentException if entity is null
     */
    public StockConsignmentView toView(StockConsignmentViewEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("StockConsignmentViewEntity cannot be null");
        }

        // Convert line items
        List<ConsignmentLineItem> lineItems = new ArrayList<>();
        if (entity.getLineItems() != null && !entity.getLineItems().isEmpty()) {
            for (ConsignmentLineItemEntity lineItemEntity : entity.getLineItems()) {
                ConsignmentLineItem lineItem = ConsignmentLineItem.builder().productCode(ProductCode.of(lineItemEntity.getProductCode())).quantity(lineItemEntity.getQuantity())
                        .expirationDate(lineItemEntity.getExpirationDate()).build();
                lineItems.add(lineItem);
            }
        }

        // Build StockConsignmentView
        return StockConsignmentView.builder().consignmentId(ConsignmentId.of(entity.getId())).tenantId(entity.getTenantId())
                .consignmentReference(ConsignmentReference.of(entity.getConsignmentReference())).warehouseId(WarehouseId.of(entity.getWarehouseId())).status(entity.getStatus())
                .receivedAt(entity.getReceivedAt()).confirmedAt(entity.getConfirmedAt()).receivedBy(entity.getReceivedBy() != null ? ReceivedBy.of(entity.getReceivedBy()) : null)
                .lineItems(lineItems).createdAt(entity.getCreatedAt()).lastModifiedAt(entity.getLastModifiedAt()).build();
    }
}

