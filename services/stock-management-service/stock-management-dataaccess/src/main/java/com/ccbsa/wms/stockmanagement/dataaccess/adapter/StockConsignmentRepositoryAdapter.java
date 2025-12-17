package com.ccbsa.wms.stockmanagement.dataaccess.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stockmanagement.application.service.port.repository.StockConsignmentRepository;
import com.ccbsa.wms.stockmanagement.dataaccess.entity.StockConsignmentEntity;
import com.ccbsa.wms.stockmanagement.dataaccess.jpa.StockConsignmentJpaRepository;
import com.ccbsa.wms.stockmanagement.dataaccess.mapper.StockConsignmentEntityMapper;
import com.ccbsa.wms.stockmanagement.domain.core.entity.StockConsignment;
import com.ccbsa.wms.stockmanagement.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stockmanagement.domain.core.valueobject.ConsignmentReference;

/**
 * Repository Adapter: StockConsignmentRepositoryAdapter
 * <p>
 * Implements StockConsignmentRepository port interface. Adapts between domain StockConsignment aggregate and JPA StockConsignmentEntity.
 */
@Repository
public class StockConsignmentRepositoryAdapter
        implements StockConsignmentRepository {
    private final StockConsignmentJpaRepository jpaRepository;
    private final StockConsignmentEntityMapper mapper;

    public StockConsignmentRepositoryAdapter(StockConsignmentJpaRepository jpaRepository, StockConsignmentEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(StockConsignment consignment) {
        // Check if entity already exists to handle version correctly
        Optional<StockConsignmentEntity> existingEntity = jpaRepository.findByTenantIdAndId(consignment.getTenantId()
                .getValue(), consignment.getId()
                .getValue());

        StockConsignmentEntity entity;
        if (existingEntity.isPresent()) {
            // Update existing entity - preserve JPA managed state and version
            entity = existingEntity.get();
            updateEntityFromDomain(entity, consignment);
        } else {
            // New entity - create from domain model
            entity = mapper.toEntity(consignment);
        }

        jpaRepository.save(entity);

        // Domain events are preserved by the command handler before calling save()
        // The command handler gets domain events from the original consignment before save()
        // and publishes them after transaction commit.
    }

    /**
     * Updates an existing entity with values from the domain model. Preserves JPA managed state and version for optimistic locking.
     *
     * @param entity      Existing JPA entity
     * @param consignment Domain consignment aggregate
     */
    private void updateEntityFromDomain(StockConsignmentEntity entity, StockConsignment consignment) {
        entity.setConsignmentReference(consignment.getConsignmentReference()
                .getValue());
        entity.setWarehouseId(consignment.getWarehouseId()
                .getValue());
        entity.setStatus(consignment.getStatus());
        entity.setReceivedAt(consignment.getReceivedAt());
        entity.setConfirmedAt(consignment.getConfirmedAt());
        entity.setReceivedBy(consignment.getReceivedBy());
        entity.setLastModifiedAt(consignment.getLastModifiedAt());

        // Update line items - remove all existing and add new ones
        entity.getLineItems()
                .clear();
        StockConsignmentEntity newEntity = mapper.toEntity(consignment);
        entity.getLineItems()
                .addAll(newEntity.getLineItems());
        // Update line item references to point to this entity
        entity.getLineItems()
                .forEach(lineItem -> lineItem.setConsignment(entity));

        // Version is managed by JPA - don't update it manually
    }

    @Override
    public Optional<StockConsignment> findByIdAndTenantId(ConsignmentId id, TenantId tenantId) {
        return jpaRepository.findByTenantIdAndId(tenantId.getValue(), id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<StockConsignment> findByConsignmentReferenceAndTenantId(ConsignmentReference reference, TenantId tenantId) {
        return jpaRepository.findByTenantIdAndConsignmentReference(tenantId.getValue(), reference.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByConsignmentReferenceAndTenantId(ConsignmentReference reference, TenantId tenantId) {
        return jpaRepository.existsByTenantIdAndConsignmentReference(tenantId.getValue(), reference.getValue());
    }
}

