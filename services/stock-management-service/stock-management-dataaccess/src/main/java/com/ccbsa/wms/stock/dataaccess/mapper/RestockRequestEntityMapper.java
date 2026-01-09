package com.ccbsa.wms.stock.dataaccess.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.BigDecimalQuantity;
import com.ccbsa.common.domain.valueobject.MaximumQuantity;
import com.ccbsa.common.domain.valueobject.MinimumQuantity;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.dataaccess.entity.RestockRequestEntity;
import com.ccbsa.wms.stock.domain.core.entity.RestockRequest;
import com.ccbsa.wms.stock.domain.core.valueobject.RestockRequestId;

/**
 * Mapper: RestockRequestEntityMapper
 * <p>
 * Maps between RestockRequest domain aggregate and RestockRequestEntity JPA entity.
 * Handles conversion between domain value objects and JPA entity fields.
 */
@Component
public class RestockRequestEntityMapper {

    /**
     * Converts RestockRequest domain entity to RestockRequestEntity JPA entity.
     *
     * @param restockRequest RestockRequest domain entity
     * @return RestockRequestEntity JPA entity
     * @throws IllegalArgumentException if restockRequest is null
     */
    public RestockRequestEntity toEntity(RestockRequest restockRequest) {
        if (restockRequest == null) {
            throw new IllegalArgumentException("RestockRequest cannot be null");
        }

        RestockRequestEntity entity = new RestockRequestEntity();
        entity.setId(restockRequest.getId().getValue());
        entity.setTenantId(restockRequest.getTenantId().getValue());
        entity.setProductId(restockRequest.getProductId().getValue());
        entity.setLocationId(restockRequest.getLocationId() != null ? restockRequest.getLocationId().getValue() : null);
        entity.setCurrentQuantity(restockRequest.getCurrentQuantity().getValue());
        entity.setMinimumQuantity(restockRequest.getMinimumQuantity().getValue());
        entity.setMaximumQuantity(restockRequest.getMaximumQuantity() != null ? restockRequest.getMaximumQuantity().getValue() : null);
        entity.setRequestedQuantity(restockRequest.getRequestedQuantity().getValue());
        entity.setPriority(restockRequest.getPriority());
        entity.setStatus(restockRequest.getStatus());
        entity.setCreatedAt(restockRequest.getCreatedAt());
        entity.setSentToD365At(restockRequest.getSentToD365At());
        entity.setD365OrderReference(restockRequest.getD365OrderReference());

        // Set version for optimistic locking
        int domainVersion = restockRequest.getVersion();
        if (domainVersion > 0) {
            entity.setVersion(Long.valueOf(domainVersion));
        }

        return entity;
    }

    /**
     * Converts RestockRequestEntity JPA entity to RestockRequest domain entity.
     *
     * @param entity RestockRequestEntity JPA entity
     * @return RestockRequest domain entity
     * @throws IllegalArgumentException if entity is null
     */
    public RestockRequest toDomain(RestockRequestEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("RestockRequestEntity cannot be null");
        }

        RestockRequest.Builder builder = RestockRequest.builder().restockRequestId(RestockRequestId.of(entity.getId())).tenantId(TenantId.of(entity.getTenantId()))
                .productId(ProductId.of(entity.getProductId())).currentQuantity(BigDecimalQuantity.of(entity.getCurrentQuantity()))
                .minimumQuantity(MinimumQuantity.of(entity.getMinimumQuantity())).requestedQuantity(BigDecimalQuantity.of(entity.getRequestedQuantity()))
                .priority(entity.getPriority()).status(entity.getStatus()).createdAt(entity.getCreatedAt()).sentToD365At(entity.getSentToD365At())
                .d365OrderReference(entity.getD365OrderReference()).version(entity.getVersion() != null ? entity.getVersion().intValue() : 0);

        // Set optional location ID
        if (entity.getLocationId() != null) {
            builder.locationId(LocationId.of(entity.getLocationId()));
        }

        // Set optional maximum quantity
        if (entity.getMaximumQuantity() != null) {
            builder.maximumQuantity(MaximumQuantity.of(entity.getMaximumQuantity()));
        }

        return builder.buildWithoutEvents();
    }
}
