package com.ccbsa.wms.returns.dataaccess.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.Notes;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.dataaccess.entity.DamageAssessmentEntity;
import com.ccbsa.wms.returns.dataaccess.entity.DamagedProductItemEntity;
import com.ccbsa.wms.returns.domain.core.entity.DamageAssessment;
import com.ccbsa.wms.returns.domain.core.entity.DamagedProductItem;
import com.ccbsa.wms.returns.domain.core.valueobject.DamageAssessmentId;
import com.ccbsa.wms.returns.domain.core.valueobject.DamagedProductItemId;
import com.ccbsa.wms.returns.domain.core.valueobject.InsuranceClaimInfo;

/**
 * Mapper: DamageAssessmentEntityMapper
 * <p>
 * Maps between DamageAssessment domain aggregate and DamageAssessmentEntity JPA entity. Handles conversion between domain value objects and JPA entity fields.
 */
@Component
public class DamageAssessmentEntityMapper {

    /**
     * Converts DamageAssessment domain entity to DamageAssessmentEntity JPA entity.
     *
     * @param damageAssessment DamageAssessment domain entity
     * @return DamageAssessmentEntity JPA entity
     * @throws IllegalArgumentException if damageAssessment is null
     */
    public DamageAssessmentEntity toEntity(DamageAssessment damageAssessment) {
        if (damageAssessment == null) {
            throw new IllegalArgumentException("DamageAssessment cannot be null");
        }

        DamageAssessmentEntity entity = new DamageAssessmentEntity();
        entity.setAssessmentId(damageAssessment.getId().getValue());
        entity.setTenantId(damageAssessment.getTenantId().getValue());
        entity.setOrderNumber(damageAssessment.getOrderNumber().getValue());
        entity.setDamageType(damageAssessment.getDamageType());
        entity.setDamageSeverity(damageAssessment.getDamageSeverity());
        entity.setDamageSource(damageAssessment.getDamageSource());
        entity.setAssessmentStatus(damageAssessment.getStatus());
        entity.setDamageNotes(damageAssessment.getDamageNotes() != null ? damageAssessment.getDamageNotes().getValue() : null);
        entity.setRecordedAt(damageAssessment.getRecordedAt());
        entity.setCreatedAt(damageAssessment.getCreatedAt());
        entity.setLastModifiedAt(damageAssessment.getLastModifiedAt());

        // Map insurance claim info
        if (damageAssessment.getInsuranceClaimInfo() != null) {
            InsuranceClaimInfo claimInfo = damageAssessment.getInsuranceClaimInfo();
            entity.setInsuranceClaimNumber(claimInfo.getClaimNumber());
            entity.setInsuranceCompany(claimInfo.getInsuranceCompany());
            entity.setInsuranceClaimStatus(claimInfo.getClaimStatus());
            entity.setInsuranceClaimAmount(claimInfo.getClaimAmount());
        }

        // Map damaged product items
        List<DamagedProductItemEntity> damagedProductItemEntities = new ArrayList<>();
        if (damageAssessment.getDamagedProductItems() != null) {
            for (DamagedProductItem damagedProductItem : damageAssessment.getDamagedProductItems()) {
                DamagedProductItemEntity itemEntity = new DamagedProductItemEntity();
                itemEntity.setItemId(damagedProductItem.getId().getValue());
                itemEntity.setDamageAssessment(entity);
                itemEntity.setProductId(damagedProductItem.getProductId().getValue());
                itemEntity.setDamagedQuantity(damagedProductItem.getDamagedQuantity().getValue());
                itemEntity.setDamageType(damagedProductItem.getDamageType());
                itemEntity.setDamageSeverity(damagedProductItem.getDamageSeverity());
                itemEntity.setDamageSource(damagedProductItem.getDamageSource());
                itemEntity.setPhotoUrl(damagedProductItem.getPhotoUrl());
                itemEntity.setNotes(damagedProductItem.getNotes() != null ? damagedProductItem.getNotes().getValue() : null);
                itemEntity.setCreatedAt(LocalDateTime.now());
                damagedProductItemEntities.add(itemEntity);
            }
        }
        entity.setDamagedProductItems(damagedProductItemEntities);

        // Set version for optimistic locking
        int domainVersion = damageAssessment.getVersion();
        if (domainVersion > 0) {
            entity.setVersion(Long.valueOf(domainVersion));
        }

        return entity;
    }

    /**
     * Converts DamageAssessmentEntity JPA entity to DamageAssessment domain entity.
     *
     * @param entity DamageAssessmentEntity JPA entity
     * @return DamageAssessment domain entity
     * @throws IllegalArgumentException if entity is null
     */
    public DamageAssessment toDomain(DamageAssessmentEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("DamageAssessmentEntity cannot be null");
        }

        // Convert damaged product items
        List<DamagedProductItem> damagedProductItems = new ArrayList<>();
        if (entity.getDamagedProductItems() != null) {
            for (DamagedProductItemEntity itemEntity : entity.getDamagedProductItems()) {
                Notes itemNotes = itemEntity.getNotes() != null ? Notes.forLineItem(itemEntity.getNotes()) : Notes.forLineItem(null);
                DamagedProductItem damagedProductItem = DamagedProductItem.create(DamagedProductItemId.of(itemEntity.getItemId()), ProductId.of(itemEntity.getProductId()),
                        Quantity.of(itemEntity.getDamagedQuantity()), itemEntity.getDamageType(), itemEntity.getDamageSeverity(), itemEntity.getDamageSource(),
                        itemEntity.getPhotoUrl(), itemNotes);
                damagedProductItems.add(damagedProductItem);
            }
        }

        // Build insurance claim info if present
        InsuranceClaimInfo insuranceClaimInfo = null;
        if (entity.getInsuranceClaimNumber() != null && entity.getInsuranceCompany() != null) {
            insuranceClaimInfo =
                    InsuranceClaimInfo.of(entity.getInsuranceClaimNumber(), entity.getInsuranceCompany(), entity.getInsuranceClaimStatus(), entity.getInsuranceClaimAmount());
        }

        // Build damage notes
        Notes damageNotes = entity.getDamageNotes() != null ? Notes.of(entity.getDamageNotes()) : Notes.of(null);

        // Build domain entity using builder
        DamageAssessment damageAssessment =
                DamageAssessment.builder().damageAssessmentId(DamageAssessmentId.of(entity.getAssessmentId())).tenantId(TenantId.of(entity.getTenantId()))
                        .orderNumber(OrderNumber.of(entity.getOrderNumber())).damageType(entity.getDamageType()).damageSeverity(entity.getDamageSeverity())
                        .damageSource(entity.getDamageSource()).damagedProductItems(damagedProductItems).insuranceClaimInfo(insuranceClaimInfo).status(entity.getAssessmentStatus())
                        .damageNotes(damageNotes).recordedAt(entity.getRecordedAt()).createdAt(entity.getCreatedAt()).lastModifiedAt(entity.getLastModifiedAt())
                        .version(entity.getVersion() != null ? entity.getVersion().intValue() : 0).buildWithoutEvents();

        return damageAssessment;
    }
}
