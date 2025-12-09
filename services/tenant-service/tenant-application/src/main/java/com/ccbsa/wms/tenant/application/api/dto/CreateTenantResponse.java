package com.ccbsa.wms.tenant.application.api.dto;

/**
 * DTO: CreateTenantResponse
 * <p>
 * Response DTO for tenant creation.
 */
public final class CreateTenantResponse {
    private final String tenantId;
    private final boolean success;
    private final String message;

    public CreateTenantResponse(String tenantId,
                                boolean success,
                                String message) {
        this.tenantId = tenantId;
        this.success = success;
        this.message = message;
    }

    public String getTenantId() {
        return tenantId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}

