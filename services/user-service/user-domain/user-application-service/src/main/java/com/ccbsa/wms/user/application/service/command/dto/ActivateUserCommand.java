package com.ccbsa.wms.user.application.service.command.dto;

import java.util.Objects;

import com.ccbsa.common.domain.valueobject.UserId;

/**
 * Command DTO for activating a user.
 */
public class ActivateUserCommand {
    private final UserId userId;

    public ActivateUserCommand(UserId userId) {
        this.userId = Objects.requireNonNull(userId, "UserId is required");
    }

    public UserId getUserId() {
        return userId;
    }
}

