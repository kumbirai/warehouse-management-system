package com.ccbsa.wms.user.application.service.command.dto;

import java.util.Objects;

import com.ccbsa.common.domain.valueobject.UserId;

/**
 * Command DTO for updating user profile.
 */
public class UpdateUserProfileCommand {
    private final UserId userId;
    private final String email;
    private final String firstName;
    private final String lastName;

    public UpdateUserProfileCommand(UserId userId, String email, String firstName, String lastName) {
        this.userId = Objects.requireNonNull(userId, "UserId is required");
        this.email = Objects.requireNonNull(email, "EmailAddress is required");
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public UserId getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}

