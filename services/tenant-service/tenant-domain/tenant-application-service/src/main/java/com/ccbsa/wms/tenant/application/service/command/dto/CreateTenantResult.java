package com.ccbsa.wms.tenant.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Result: CreateTenantResult
 * <p>
 * Result of creating a tenant.
 */
public final class CreateTenantResult {
    private final TenantId tenantId;
    private final boolean success;
    private final String message;

    public CreateTenantResult(TenantId tenantId,
                              boolean success,
                              String message) {
        this.tenantId = tenantId;
        this.success = success;
        this.message = message;
    }

    public static CreateTenantResult success(TenantId tenantId) {
        return new CreateTenantResult(tenantId,
                true,
                "Tenant created successfully");
    }

    public static CreateTenantResult failure(TenantId tenantId,
                                             String message) {
        return new CreateTenantResult(tenantId,
                false,
                message);
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}

