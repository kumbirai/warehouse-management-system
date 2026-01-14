package com.ccbsa.wms.returns.dataaccess.mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.Notes;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.ReturnReason;
import com.ccbsa.common.domain.valueobject.ReturnStatus;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.dataaccess.entity.ReturnEntity;
import com.ccbsa.wms.returns.dataaccess.entity.ReturnLineItemEntity;
import com.ccbsa.wms.returns.domain.core.entity.Return;
import com.ccbsa.wms.returns.domain.core.entity.ReturnLineItem;
import com.ccbsa.wms.returns.domain.core.valueobject.CustomerSignature;
import com.ccbsa.common.domain.valueobject.ProductCondition;
import com.ccbsa.common.domain.valueobject.ReturnId;
import com.ccbsa.common.domain.valueobject.ReturnLineItemId;
import com.ccbsa.wms.returns.domain.core.valueobject.ReturnType;

/**
 * Mapper: ReturnEntityMapper
 * <p>
 * Maps between Return domain aggregate and ReturnEntity JPA entity. Handles conversion between domain value objects and JPA entity fields.
 */
@Component
public class ReturnEntityMapper {

    /**
     * Converts Return domain entity to ReturnEntity JPA entity.
     *
     * @param returnAggregate Return domain entity
     * @return ReturnEntity JPA entity
     * @throws IllegalArgumentException if returnAggregate is null
     */
    public ReturnEntity toEntity(Return returnAggregate) {
        if (returnAggregate == null) {
            throw new IllegalArgumentException("Return cannot be null");
        }

        ReturnEntity entity = new ReturnEntity();
        entity.setReturnId(returnAggregate.getId().getValue());
        entity.setTenantId(returnAggregate.getTenantId().getValue());
        entity.setOrderNumber(returnAggregate.getOrderNumber().getValue());
        entity.setReturnType(returnAggregate.getReturnType());
        entity.setReturnStatus(returnAggregate.getStatus());
        entity.setReturnedAt(returnAggregate.getReturnedAt());
        entity.setCreatedAt(returnAggregate.getCreatedAt());
        entity.setLastModifiedAt(returnAggregate.getLastModifiedAt());

        // Map customer signature
        if (returnAggregate.getCustomerSignature() != null) {
            entity.setCustomerSignature(returnAggregate.getCustomerSignature().getSignatureData());
            entity.setSignatureTimestamp(returnAggregate.getCustomerSignature().getTimestamp());
        }

        // Map primary return reason
        if (returnAggregate.getPrimaryReturnReason() != null) {
            entity.setPrimaryReturnReason(returnAggregate.getPrimaryReturnReason().name());
        }

        // Map return notes
        entity.setReturnNotes(returnAggregate.getReturnNotes() != null ? returnAggregate.getReturnNotes().getValue() : null);

        // Map line items
        List<ReturnLineItemEntity> lineItemEntities = new ArrayList<>();
        if (returnAggregate.getLineItems() != null) {
            for (ReturnLineItem lineItem : returnAggregate.getLineItems()) {
                ReturnLineItemEntity lineItemEntity = new ReturnLineItemEntity();
                lineItemEntity.setLineItemId(lineItem.getId().getValue());
                lineItemEntity.setReturnEntity(entity);
                lineItemEntity.setProductId(lineItem.getProductId().getValue());
                lineItemEntity.setOrderedQuantity(lineItem.getOrderedQuantity().getValue());
                lineItemEntity.setPickedQuantity(lineItem.getPickedQuantity().getValue());
                lineItemEntity.setAcceptedQuantity(lineItem.getAcceptedQuantity().getValue());
                lineItemEntity.setReturnedQuantity(lineItem.getReturnedQuantity().getValue());
                lineItemEntity.setProductCondition(lineItem.getProductCondition());
                lineItemEntity.setReturnReason(lineItem.getReturnReason());
                lineItemEntity.setLineNotes(lineItem.getLineNotes() != null ? lineItem.getLineNotes().getValue() : null);
                lineItemEntity.setCreatedAt(lineItem.getCreatedAt());
                lineItemEntities.add(lineItemEntity);
            }
        }
        entity.setLineItems(lineItemEntities);

        // Set version for optimistic locking
        int domainVersion = returnAggregate.getVersion();
        if (domainVersion > 0) {
            entity.setVersion(Long.valueOf(domainVersion));
        }

        return entity;
    }

    /**
     * Converts ReturnEntity JPA entity to Return domain entity.
     *
     * @param entity ReturnEntity JPA entity
     * @return Return domain entity
     * @throws IllegalArgumentException if entity is null
     */
    public Return toDomain(ReturnEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("ReturnEntity cannot be null");
        }

        // Convert line items
        List<ReturnLineItem> lineItems = new ArrayList<>();
        if (entity.getLineItems() != null) {
            for (ReturnLineItemEntity lineItemEntity : entity.getLineItems()) {
                ReturnLineItem lineItem;
                Notes lineNotes = lineItemEntity.getLineNotes() != null ? Notes.forLineItem(lineItemEntity.getLineNotes()) : Notes.forLineItem(null);
                if (lineItemEntity.getProductCondition() != null) {
                    // Full return
                    lineItem = ReturnLineItem.createFull(ReturnLineItemId.of(lineItemEntity.getLineItemId()), ProductId.of(lineItemEntity.getProductId()),
                            Quantity.of(lineItemEntity.getOrderedQuantity()), Quantity.of(lineItemEntity.getPickedQuantity()), lineItemEntity.getProductCondition(),
                            lineItemEntity.getReturnReason(), lineNotes);
                } else {
                    // Partial return
                    lineItem = ReturnLineItem.createPartial(ReturnLineItemId.of(lineItemEntity.getLineItemId()), ProductId.of(lineItemEntity.getProductId()),
                            Quantity.of(lineItemEntity.getOrderedQuantity()), Quantity.of(lineItemEntity.getPickedQuantity()), Quantity.of(lineItemEntity.getAcceptedQuantity()),
                            lineItemEntity.getReturnReason(), lineNotes);
                }
                lineItems.add(lineItem);
            }
        }

        // Build customer signature if present
        CustomerSignature customerSignature = null;
        if (entity.getCustomerSignature() != null && entity.getSignatureTimestamp() != null) {
            customerSignature = CustomerSignature.of(entity.getCustomerSignature(), entity.getSignatureTimestamp());
        }

        // Build primary return reason if present
        ReturnReason primaryReturnReason = null;
        if (entity.getPrimaryReturnReason() != null) {
            primaryReturnReason = ReturnReason.valueOf(entity.getPrimaryReturnReason());
        }

        // Build return notes
        Notes returnNotes = entity.getReturnNotes() != null ? Notes.of(entity.getReturnNotes()) : Notes.of(null);

        // Build domain entity using builder
        Return returnAggregate =
                Return.builder().returnId(ReturnId.of(entity.getReturnId())).tenantId(TenantId.of(entity.getTenantId())).orderNumber(OrderNumber.of(entity.getOrderNumber()))
                        .returnType(entity.getReturnType()).status(entity.getReturnStatus()).lineItems(lineItems).customerSignature(customerSignature)
                        .primaryReturnReason(primaryReturnReason).returnNotes(returnNotes).returnedAt(entity.getReturnedAt()).createdAt(entity.getCreatedAt())
                        .lastModifiedAt(entity.getLastModifiedAt()).version(entity.getVersion() != null ? entity.getVersion().intValue() : 0).buildWithoutEvents();

        return returnAggregate;
    }
}
