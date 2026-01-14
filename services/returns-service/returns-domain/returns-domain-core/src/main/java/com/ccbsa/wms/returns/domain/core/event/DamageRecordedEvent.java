package com.ccbsa.wms.returns.domain.core.event;

import java.util.List;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.DamageSeverity;
import com.ccbsa.common.domain.valueobject.DamageSource;
import com.ccbsa.common.domain.valueobject.DamageType;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.domain.core.entity.DamageAssessment;
import com.ccbsa.wms.returns.domain.core.entity.DamagedProductItem;

/**
 * Domain Event: DamageRecordedEvent
 * <p>
 * Published when a damage assessment is recorded.
 * <p>
 * This event indicates that:
 * - A new damage assessment has been created
 * - Damage details have been recorded
 * - Damaged product items have been identified
 */
public class DamageRecordedEvent extends ReturnsEvent<DamageAssessment> {
    private static final String AGGREGATE_TYPE = "DamageAssessment";

    private final OrderNumber orderNumber;
    private final TenantId tenantId;
    private final DamageType damageType;
    private final DamageSeverity damageSeverity;
    private final DamageSource damageSource;
    private final List<DamagedProductItem> damagedProductItems;

    /**
     * Constructor for DamageRecordedEvent.
     *
     * @param aggregateId         Damage assessment ID (as String)
     * @param orderNumber         Order number
     * @param tenantId            Tenant identifier
     * @param damageType          Damage type
     * @param damageSeverity      Damage severity
     * @param damageSource        Damage source
     * @param damagedProductItems List of damaged product items
     */
    public DamageRecordedEvent(String aggregateId, OrderNumber orderNumber, TenantId tenantId, DamageType damageType, DamageSeverity damageSeverity, DamageSource damageSource,
                               List<DamagedProductItem> damagedProductItems) {
        super(aggregateId, AGGREGATE_TYPE);
        this.orderNumber = orderNumber;
        this.tenantId = tenantId;
        this.damageType = damageType;
        this.damageSeverity = damageSeverity;
        this.damageSource = damageSource;
        this.damagedProductItems = damagedProductItems != null ? List.copyOf(damagedProductItems) : List.of();
    }

    /**
     * Constructor for DamageRecordedEvent with metadata.
     *
     * @param aggregateId         Damage assessment ID (as String)
     * @param orderNumber         Order number
     * @param tenantId            Tenant identifier
     * @param damageType          Damage type
     * @param damageSeverity      Damage severity
     * @param damageSource        Damage source
     * @param damagedProductItems List of damaged product items
     * @param metadata            Event metadata for traceability
     */
    public DamageRecordedEvent(String aggregateId, OrderNumber orderNumber, TenantId tenantId, DamageType damageType, DamageSeverity damageSeverity, DamageSource damageSource,
                               List<DamagedProductItem> damagedProductItems, EventMetadata metadata) {
        super(aggregateId, AGGREGATE_TYPE, metadata);
        this.orderNumber = orderNumber;
        this.tenantId = tenantId;
        this.damageType = damageType;
        this.damageSeverity = damageSeverity;
        this.damageSource = damageSource;
        this.damagedProductItems = damagedProductItems != null ? List.copyOf(damagedProductItems) : List.of();
    }

    public OrderNumber getOrderNumber() {
        return orderNumber;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public DamageType getDamageType() {
        return damageType;
    }

    public DamageSeverity getDamageSeverity() {
        return damageSeverity;
    }

    public DamageSource getDamageSource() {
        return damageSource;
    }

    public List<DamagedProductItem> getDamagedProductItems() {
        return damagedProductItems;
    }
}
