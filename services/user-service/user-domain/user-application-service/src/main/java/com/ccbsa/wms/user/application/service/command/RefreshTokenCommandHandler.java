package com.ccbsa.wms.user.application.service.command;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.user.application.service.command.dto.AuthenticationResult;
import com.ccbsa.wms.user.application.service.command.dto.RefreshTokenCommand;
import com.ccbsa.wms.user.application.service.port.auth.AuthenticationServicePort;

/**
 * Command Handler: RefreshTokenCommandHandler
 * <p>
 * Handles token refresh command.
 * <p>
 * Responsibilities:
 * - Execute token refresh via authentication service port
 * - Return authentication result with new tokens and user context
 */
@Component
public class RefreshTokenCommandHandler {
    private final AuthenticationServicePort authenticationService;

    public RefreshTokenCommandHandler(AuthenticationServicePort authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Handles the RefreshTokenCommand.
     * <p>
     * Transaction boundary: One transaction per command execution.
     *
     * @param command Command to execute
     * @return Authentication result with new tokens and user context
     * @throws com.ccbsa.wms.user.application.service.exception.AuthenticationException if refresh token is invalid or expired
     */
    @Transactional
    public AuthenticationResult handle(RefreshTokenCommand command) {
        return authenticationService.refreshToken(command);
    }
}

