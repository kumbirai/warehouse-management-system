package com.ccbsa.wms.user.application.service.query.dto;

import com.ccbsa.common.application.query.Query;

/**
 * Query: UserContextQuery
 * <p>
 * Query to get user context from JWT token.
 */
public final class UserContextQuery implements Query<UserContextView> {
    private final String accessToken;

    public UserContextQuery(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token cannot be null or blank");
        }
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public String toString() {
        return "UserContextQuery{accessToken='***'}";
    }
}

