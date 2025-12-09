package com.ccbsa.wms.user.application.service.command.dto;

import java.util.Objects;

/**
 * Result DTO for user creation command.
 */
public class CreateUserResult {
    private final String userId;
    private final boolean success;
    private final String message;

    public CreateUserResult(String userId, boolean success, String message) {
        this.userId = Objects.requireNonNull(userId, "UserId is required");
        this.success = success;
        this.message = Objects.requireNonNull(message, "Message is required");
    }

    public String getUserId() {
        return userId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}

