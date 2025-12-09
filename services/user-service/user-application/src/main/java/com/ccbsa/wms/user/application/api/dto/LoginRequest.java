package com.ccbsa.wms.user.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO: LoginRequest
 * <p>
 * Request DTO for user login via BFF.
 * <p>
 * Validation rules:
 * - Username: 1-255 characters, alphanumeric, dots, underscores, hyphens, @ symbol
 * - Password: 1-255 characters (no pattern to allow special characters)
 */
public final class LoginRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 1,
            max = 255,
            message = "Username must be between 1 and 255 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._@-]+$",
            message = "Username contains invalid characters. Only letters, numbers, dots, underscores, hyphens, and @ are allowed")
    private String username;
    @NotBlank(message = "Password is required")
    @Size(min = 1,
            max = 255,
            message = "Password must be between 1 and 255 characters")
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String username,
                        String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return String.format("LoginRequest{username='%s'}",
                username);
    }
}

