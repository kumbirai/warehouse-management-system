package com.ccbsa.wms.tenant.application.service.command.dto;

import com.ccbsa.common.application.command.Command;
import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Command: SuspendTenantCommand
 * <p>
 * Represents the intent to suspend a tenant.
 */
public final class SuspendTenantCommand
        implements Command {
    private final TenantId tenantId;

    public SuspendTenantCommand(TenantId tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId cannot be null");
        }
        this.tenantId = tenantId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }
}

