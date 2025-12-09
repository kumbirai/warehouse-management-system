package com.ccbsa.wms.user.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.user.domain.core.valueobject.UserStatus;

/**
 * Query DTO for listing users.
 */
public class ListUsersQuery {
    private final TenantId tenantId;
    private final UserStatus status;
    private final Integer page;
    private final Integer size;

    public ListUsersQuery(TenantId tenantId, UserStatus status, Integer page, Integer size) {
        this.tenantId = tenantId; // Can be null for SYSTEM_ADMIN
        this.status = status;
        this.page = page != null && page >= 0 ? page : 0;
        this.size = size != null && size > 0 ? size : 20;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getSize() {
        return size;
    }
}

