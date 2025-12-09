package com.ccbsa.wms.user.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.user.application.service.port.auth.AuthenticationServicePort;
import com.ccbsa.wms.user.application.service.query.dto.UserContextQuery;
import com.ccbsa.wms.user.application.service.query.dto.UserContextView;

/**
 * Query Handler: GetUserContextQueryHandler
 * <p>
 * Handles user context query.
 * <p>
 * Responsibilities:
 * - Extract user context from JWT token
 * - Return user context view
 */
@Component
public class GetUserContextQueryHandler {
    private final AuthenticationServicePort authenticationService;

    public GetUserContextQueryHandler(AuthenticationServicePort authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Handles the UserContextQuery.
     * <p>
     * Transaction boundary: Read-only transaction for query operation.
     *
     * @param query Query to execute
     * @return User context view
     */
    @Transactional(readOnly = true)
    public UserContextView handle(UserContextQuery query) {
        return authenticationService.getUserContext(query);
    }
}

