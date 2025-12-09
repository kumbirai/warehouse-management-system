package com.ccbsa.wms.user.application.service.query.dto;

import java.util.Objects;

import com.ccbsa.common.domain.valueobject.UserId;

/**
 * Query DTO for getting a user by ID.
 */
public class GetUserQuery {
    private final UserId userId;

    public GetUserQuery(UserId userId) {
        this.userId = Objects.requireNonNull(userId, "UserId is required");
    }

    public UserId getUserId() {
        return userId;
    }
}

