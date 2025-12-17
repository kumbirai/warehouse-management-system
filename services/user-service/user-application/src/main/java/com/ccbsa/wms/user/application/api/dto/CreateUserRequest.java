package com.ccbsa.wms.user.application.api.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a user.
 */
public class CreateUserRequest {
    @NotBlank(message = "TenantId is required")
    private String tenantId;

    @NotBlank(message = "Username is required")
    @Size(min = 3,
            max = 100,
            message = "Username must be between 3 and 100 characters")
    private String username;

    @NotBlank(message = "EmailAddress is required")
    @Email(message = "EmailAddress must be valid")
    @Size(max = 255,
            message = "EmailAddress cannot exceed 255 characters")
    @JsonProperty("emailAddress")
    private String emailAddress;

    @NotBlank(message = "Password is required")
    @Size(min = 8,
            message = "Password must be at least 8 characters")
    private String password;

    @Size(max = 100,
            message = "First name cannot exceed 100 characters")
    private String firstName;

    @Size(max = 100,
            message = "Last name cannot exceed 100 characters")
    private String lastName;

    private List<String> roles;

    // Getters and setters
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public List<String> getRoles() {
        // Return unmodifiable list to prevent external modification
        return roles != null ? Collections.unmodifiableList(roles) : Collections.emptyList();
    }

    public void setRoles(List<String> roles) {
        // Defensive copy to prevent external modification
        this.roles = roles != null ? new ArrayList<>(roles) : null;
    }
}

