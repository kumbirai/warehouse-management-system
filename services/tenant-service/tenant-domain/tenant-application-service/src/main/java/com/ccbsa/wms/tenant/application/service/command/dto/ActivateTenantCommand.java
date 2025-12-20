package com.ccbsa.wms.tenant.application.service.command.dto;

import com.ccbsa.common.application.command.Command;
import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Command: ActivateTenantCommand
 * <p>
 * Represents the intent to activate a tenant.
 */
public final class ActivateTenantCommand implements Command {
    private final TenantId tenantId;

    public ActivateTenantCommand(TenantId tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId cannot be null");
        }
        this.tenantId = tenantId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }
}

