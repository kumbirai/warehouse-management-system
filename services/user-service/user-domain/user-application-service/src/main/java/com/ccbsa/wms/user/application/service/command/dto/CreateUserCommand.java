package com.ccbsa.wms.user.application.service.command.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Command DTO for creating a new user.
 */
public class CreateUserCommand {
    private final String tenantId;
    private final String username;
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final List<String> roles;

    public CreateUserCommand(String tenantId, String username, String email,
                             String password, String firstName, String lastName,
                             List<String> roles) {
        this.tenantId = Objects.requireNonNull(tenantId, "TenantId is required");
        this.username = Objects.requireNonNull(username, "Username is required");
        this.email = Objects.requireNonNull(email, "EmailAddress is required");
        this.password = Objects.requireNonNull(password, "Password is required");
        this.firstName = firstName;
        this.lastName = lastName;
        // Defensive copy to prevent external modification
        this.roles = roles != null ? new ArrayList<>(roles) : Collections.emptyList();
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public List<String> getRoles() {
        // Return unmodifiable list to prevent external modification
        return Collections.unmodifiableList(roles);
    }
}

