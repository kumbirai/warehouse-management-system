package com.ccbsa.wms.tenant.application.service.command.dto;

import com.ccbsa.common.application.command.Command;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantConfiguration;

/**
 * Command: UpdateTenantConfigurationCommand
 * <p>
 * Represents the intent to update tenant configuration.
 */
public final class UpdateTenantConfigurationCommand
        implements Command {
    private final TenantId tenantId;
    private final TenantConfiguration configuration;

    public UpdateTenantConfigurationCommand(TenantId tenantId, TenantConfiguration configuration) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId cannot be null");
        }
        if (configuration == null) {
            throw new IllegalArgumentException("Tenant configuration cannot be null");
        }
        this.tenantId = tenantId;
        this.configuration = configuration;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public TenantConfiguration getConfiguration() {
        return configuration;
    }
}

