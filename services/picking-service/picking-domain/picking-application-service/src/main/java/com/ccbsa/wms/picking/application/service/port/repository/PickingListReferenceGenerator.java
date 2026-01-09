package com.ccbsa.wms.picking.application.service.port.repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListReference;

/**
 * Port Interface: PickingListReferenceGenerator
 * <p>
 * Generates unique human-readable picking list references per tenant.
 * <p>
 * Format: PICK-{YYYYMMDD}-{sequence} (e.g., PICK-20250107-001)
 */
public interface PickingListReferenceGenerator {
    /**
     * Generates a unique picking list reference for the given tenant.
     * <p>
     * The reference format is: PICK-{YYYYMMDD}-{sequence}
     * where sequence is a 3-digit number (001, 002, etc.) that increments per tenant per day.
     *
     * @param tenantId Tenant identifier
     * @return Unique PickingListReference
     */
    PickingListReference generate(TenantId tenantId);
}
