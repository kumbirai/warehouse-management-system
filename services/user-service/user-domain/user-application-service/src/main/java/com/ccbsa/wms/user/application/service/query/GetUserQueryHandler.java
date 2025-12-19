package com.ccbsa.wms.user.application.service.query;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.user.application.service.exception.UserNotFoundException;
import com.ccbsa.wms.user.application.service.port.auth.AuthenticationServicePort;
import com.ccbsa.wms.user.application.service.port.repository.UserRepository;
import com.ccbsa.wms.user.application.service.port.service.TenantServicePort;
import com.ccbsa.wms.user.application.service.query.dto.GetUserQuery;
import com.ccbsa.wms.user.application.service.query.dto.GetUserQueryResult;
import com.ccbsa.wms.user.domain.core.entity.User;
import com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId;

/**
 * Query Handler: GetUserQueryHandler
 * <p>
 * Handles query for user by ID.
 * <p>
 * For SYSTEM_ADMIN users, searches across all tenant schemas using findByIdAcrossTenants.
 * For other users, searches within the current tenant schema using findById.
 */
@Component
public class GetUserQueryHandler {
    private static final Logger logger = LoggerFactory.getLogger(GetUserQueryHandler.class);

    private final UserRepository userRepository;
    private final AuthenticationServicePort authenticationService;
    private final TenantServicePort tenantServicePort;

    public GetUserQueryHandler(UserRepository userRepository, AuthenticationServicePort authenticationService, TenantServicePort tenantServicePort) {
        this.userRepository = userRepository;
        this.authenticationService = authenticationService;
        this.tenantServicePort = tenantServicePort;
    }

    /**
     * Handles the GetUserQuery.
     * <p>
     * Read-only transaction for query optimization.
     * <p>
     * For SYSTEM_ADMIN users, searches across all tenant schemas.
     * For other users, searches within the current tenant schema.
     *
     * @param query Query to execute
     * @return Query result
     * @throws UserNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public GetUserQueryResult handle(GetUserQuery query) {
        logger.debug("Getting user: userId={}, isSystemAdmin={}", query.getUserId()
                .getValue(), query.isSystemAdmin());

        // 1. Load user - use cross-tenant search for SYSTEM_ADMIN
        User user;
        if (query.isSystemAdmin()) {
            logger.debug("SYSTEM_ADMIN user detected - searching across all tenant schemas");
            user = userRepository.findByIdAcrossTenants(query.getUserId())
                    .orElseThrow(() -> new UserNotFoundException(String.format("User not found: %s", query.getUserId()
                            .getValue())));
        } else {
            logger.debug("Non-SYSTEM_ADMIN user - searching within current tenant schema");
            user = userRepository.findById(query.getUserId())
                    .orElseThrow(() -> new UserNotFoundException(String.format("User not found: %s", query.getUserId()
                            .getValue())));
        }

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

        // 3. Get tenant name from tenant service
        String tenantName = null;
        try {
            Optional<TenantServicePort.TenantInfo> tenantInfo = tenantServicePort.getTenantInfo(user.getTenantId());
            if (tenantInfo.isPresent()) {
                tenantName = tenantInfo.get().name();
                logger.debug("Retrieved tenant name: tenantId={}, name={}", user.getTenantId().getValue(), tenantName);
            } else {
                logger.warn("Tenant info not found for tenantId: {}", user.getTenantId().getValue());
            }
        } catch (Exception e) {
            logger.warn("Failed to get tenant name from tenant service: {}", e.getMessage());
            // Continue without tenant name
        }

        // 4. Map to query result
        return new GetUserQueryResult(user.getId(), user.getTenantId(), tenantName, user.getUsername()
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

