package com.ccbsa.wms.user.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO: RefreshTokenRequest
 * <p>
 * Request DTO for token refresh.
 */
public final class RefreshTokenRequest {
    @NotBlank(message = "Refresh token is required")
    @Size(min = 10, max = 5000, message = "Refresh token must be between 10 and 5000 characters")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Refresh token contains invalid characters")
    private String refreshToken;

    public RefreshTokenRequest() {
    }

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Override
    public String toString() {
        return "RefreshTokenRequest{refreshToken='***'}";
    }
}

