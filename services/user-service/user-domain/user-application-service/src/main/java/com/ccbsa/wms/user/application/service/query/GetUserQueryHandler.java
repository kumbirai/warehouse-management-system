package com.ccbsa.wms.user.application.service.query;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.user.application.service.exception.UserNotFoundException;
import com.ccbsa.wms.user.application.service.port.auth.AuthenticationServicePort;
import com.ccbsa.wms.user.application.service.port.repository.UserRepository;
import com.ccbsa.wms.user.application.service.query.dto.GetUserQuery;
import com.ccbsa.wms.user.application.service.query.dto.GetUserQueryResult;
import com.ccbsa.wms.user.domain.core.entity.User;
import com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId;

/**
 * Query Handler: GetUserQueryHandler
 * <p>
 * Handles query for user by ID.
 */
@Component
public class GetUserQueryHandler {
    private static final Logger logger = LoggerFactory.getLogger(GetUserQueryHandler.class);

    private final UserRepository userRepository;
    private final AuthenticationServicePort authenticationService;

    public GetUserQueryHandler(UserRepository userRepository, AuthenticationServicePort authenticationService) {
        this.userRepository = userRepository;
        this.authenticationService = authenticationService;
    }

    /**
     * Handles the GetUserQuery.
     * <p>
     * Read-only transaction for query optimization.
     *
     * @param query Query to execute
     * @return Query result
     * @throws UserNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public GetUserQueryResult handle(GetUserQuery query) {
        logger.debug("Getting user: userId={}", query.getUserId()
                .getValue());

        // 1. Load user
        User user = userRepository.findById(query.getUserId())
                .orElseThrow(() -> new UserNotFoundException(String.format("User not found: %s", query.getUserId()
                        .getValue())));

        // 2. Get roles from Keycloak
        List<String> roles = List.of();
        if (user.getKeycloakUserId()
                .isPresent()) {
            try {
                roles = authenticationService.getUserRoles(user.getKeycloakUserId()
                        .get());
            } catch (Exception e) {
                logger.warn("Failed to get user roles from Keycloak: {}", e.getMessage());
                // Continue without roles
            }
        }

        // 3. Map to query result
        return new GetUserQueryResult(user.getId(), user.getTenantId(), user.getUsername()
                .getValue(), user.getEmail()
                .getValue(), user.getFirstName()
                .map(fn -> fn.getValue())
                .orElse(null), user.getLastName()
                .map(ln -> ln.getValue())
                .orElse(null), user.getStatus(), user.getKeycloakUserId()
                .map(KeycloakUserId::getValue)
                .orElse(null), roles, user.getCreatedAt(), user.getLastModifiedAt());
    }
}

