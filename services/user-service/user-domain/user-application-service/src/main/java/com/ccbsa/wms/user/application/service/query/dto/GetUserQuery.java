package com.ccbsa.wms.user.application.service.query.dto;

import java.util.Objects;

import com.ccbsa.common.domain.valueobject.UserId;

/**
 * Query DTO for getting a user by ID.
 */
public class GetUserQuery {
    private final UserId userId;
    private final boolean isSystemAdmin;

    public GetUserQuery(UserId userId, boolean isSystemAdmin) {
        this.userId = Objects.requireNonNull(userId, "UserId is required");
        this.isSystemAdmin = isSystemAdmin;
    }

    public UserId getUserId() {
        return userId;
    }

    public boolean isSystemAdmin() {
        return isSystemAdmin;
    }
}

