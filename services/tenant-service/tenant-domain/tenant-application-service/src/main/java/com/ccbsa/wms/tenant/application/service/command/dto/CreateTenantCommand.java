package com.ccbsa.wms.tenant.application.service.command.dto;

import com.ccbsa.common.application.command.Command;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.domain.core.valueobject.ContactInformation;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantConfiguration;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantName;

/**
 * Command: CreateTenantCommand
 * <p>
 * Represents the intent to create a new tenant.
 */
public final class CreateTenantCommand implements Command {
    private final TenantId tenantId;
    private final TenantName name;
    private final ContactInformation contactInformation;
    private final TenantConfiguration configuration;

    public CreateTenantCommand(TenantId tenantId,
                               TenantName name,
                               ContactInformation contactInformation,
                               TenantConfiguration configuration) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Tenant name cannot be null");
        }
        this.tenantId = tenantId;
        this.name = name;
        this.contactInformation = contactInformation;
        this.configuration = configuration != null ? configuration : TenantConfiguration.defaultConfiguration();
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public TenantName getName() {
        return name;
    }

    public ContactInformation getContactInformation() {
        return contactInformation;
    }

    public TenantConfiguration getConfiguration() {
        return configuration;
    }
}

