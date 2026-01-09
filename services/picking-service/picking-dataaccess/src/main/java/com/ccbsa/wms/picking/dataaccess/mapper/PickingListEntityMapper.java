package com.ccbsa.wms.picking.dataaccess.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.picking.dataaccess.entity.LoadEntity;
import com.ccbsa.wms.picking.dataaccess.entity.PickingListEntity;
import com.ccbsa.wms.picking.domain.core.entity.Load;
import com.ccbsa.wms.picking.domain.core.entity.PickingList;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListReference;

import lombok.RequiredArgsConstructor;

/**
 * Mapper: PickingListEntityMapper
 * <p>
 * Maps between PickingList domain aggregate and PickingListEntity JPA entity.
 */
@Component
@RequiredArgsConstructor
public class PickingListEntityMapper {
    private final LoadEntityMapper loadEntityMapper;

    public PickingListEntity toEntity(PickingList pickingList) {
        if (pickingList == null) {
            throw new IllegalArgumentException("PickingList cannot be null");
        }

        PickingListEntity entity = new PickingListEntity();
        entity.setId(pickingList.getId().getValue());
        entity.setTenantId(pickingList.getTenantId().getValue());
        entity.setStatus(pickingList.getStatus());
        entity.setReceivedAt(pickingList.getReceivedAt());
        entity.setProcessedAt(pickingList.getProcessedAt());
        entity.setCompletedAt(pickingList.getCompletedAt());
        entity.setCompletedByUserId(pickingList.getCompletedByUserId() != null ? pickingList.getCompletedByUserId().getValue() : null);
        entity.setNotes(pickingList.getNotes() != null ? pickingList.getNotes().getValue() : null);
        entity.setPickingListReference(pickingList.getPickingListReference() != null ? pickingList.getPickingListReference().getValue() : null);

        // Map loads
        List<LoadEntity> loadEntities = new ArrayList<>();
        for (Load load : pickingList.getLoads()) {
            LoadEntity loadEntity = loadEntityMapper.toEntity(load);
            loadEntity.setPickingList(entity);
            loadEntities.add(loadEntity);
        }
        entity.setLoads(loadEntities);

        // Set version for optimistic locking (never set to 0 for new entities)
        int domainVersion = pickingList.getVersion();
        if (domainVersion > 0) {
            entity.setVersion(Long.valueOf(domainVersion));
        }

        return entity;
    }

    public PickingList toDomain(PickingListEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("PickingListEntity cannot be null");
        }

        // Convert loads
        List<Load> loads = new ArrayList<>();
        if (entity.getLoads() != null) {
            for (LoadEntity loadEntity : entity.getLoads()) {
                loads.add(loadEntityMapper.toDomain(loadEntity));
            }
        }

        // Build domain entity using builder
        PickingList.Builder builder = PickingList.builder().id(PickingListId.of(entity.getId())).tenantId(TenantId.of(entity.getTenantId())).loads(loads).status(entity.getStatus())
                .receivedAt(entity.getReceivedAt()).processedAt(entity.getProcessedAt()).completedAt(entity.getCompletedAt())
                .completedByUserId(entity.getCompletedByUserId() != null ? UserId.of(entity.getCompletedByUserId()) : null).notes(entity.getNotes())
                .version(entity.getVersion() != null ? entity.getVersion().intValue() : 0);

        if (entity.getPickingListReference() != null) {
            builder.pickingListReference(PickingListReference.of(entity.getPickingListReference()));
        }

        return builder.buildWithoutEvents();
    }
}
