package com.ccbsa.wms.user.application.service.command.dto;

import com.ccbsa.common.application.command.Command;

/**
 * Command: RefreshTokenCommand
 * <p>
 * Represents the intent to refresh an access token.
 */
public final class RefreshTokenCommand implements Command {
    private final String refreshToken;

    public RefreshTokenCommand(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token cannot be null or blank");
        }
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    @Override
    public String toString() {
        return "RefreshTokenCommand{refreshToken='***'}";
    }
}

