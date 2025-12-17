package com.ccbsa.wms.user.application.service.command;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.user.application.service.command.dto.AuthenticationResult;
import com.ccbsa.wms.user.application.service.command.dto.LoginCommand;
import com.ccbsa.wms.user.application.service.port.auth.AuthenticationServicePort;

/**
 * Command Handler: LoginCommandHandler
 * <p>
 * Handles user login command.
 * <p>
 * Responsibilities: - Execute authentication via authentication service port - Return authentication result with tokens and user context
 */
@Component
public class LoginCommandHandler {
    private final AuthenticationServicePort authenticationService;

    public LoginCommandHandler(AuthenticationServicePort authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Handles the LoginCommand.
     * <p>
     * Transaction boundary: One transaction per command execution.
     *
     * @param command Command to execute
     * @return Authentication result with tokens and user context
     * @throws com.ccbsa.wms.user.application.service.exception.AuthenticationException if authentication fails
     */
    @Transactional
    public AuthenticationResult handle(LoginCommand command) {
        return authenticationService.login(command);
    }
}

