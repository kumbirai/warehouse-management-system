package com.ccbsa.wms.user.application.service.query;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.user.application.service.exception.UserNotFoundException;
import com.ccbsa.wms.user.application.service.port.auth.AuthenticationServicePort;
import com.ccbsa.wms.user.application.service.port.data.UserViewRepository;
import com.ccbsa.wms.user.application.service.port.data.dto.UserView;
import com.ccbsa.wms.user.application.service.port.service.TenantServicePort;
import com.ccbsa.wms.user.application.service.query.dto.GetUserQuery;
import com.ccbsa.wms.user.application.service.query.dto.GetUserQueryResult;
import com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: GetUserQueryHandler
 * <p>
 * Handles query for user read model by ID.
 * <p>
 * For SYSTEM_ADMIN users, searches across all tenant schemas using findByIdAcrossTenants.
 * For other users, searches within the current tenant schema using findById.
 * <p>
 * Uses data port (UserViewRepository) instead of repository port for CQRS compliance.
 * <p>
 * Note: This handler still calls AuthenticationServicePort and TenantServicePort for additional
 * data (roles, tenant name) that are not part of the read model. This is acceptable as these
 * are external service calls, not repository port usage.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetUserQueryHandler {
    private final UserViewRepository viewRepository;
    private final AuthenticationServicePort authenticationService;
    private final TenantServicePort tenantServicePort;

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
        log.debug("Getting user: userId={}, isSystemAdmin={}", query.getUserId().getValue(), query.isSystemAdmin());

        // 1. Load user view - use cross-tenant search for SYSTEM_ADMIN
        Optional<UserView> userView = query.isSystemAdmin() ? viewRepository.findByIdAcrossTenants(query.getUserId()) : viewRepository.findById(query.getUserId());

        UserView view = userView.orElseThrow(() -> new UserNotFoundException(String.format("User not found: %s", query.getUserId().getValue())));

        // 2. Get roles from Keycloak
        List<String> roles = List.of();
        if (view.getKeycloakUserId() != null) {
            try {
                roles = authenticationService.getUserRoles(KeycloakUserId.of(view.getKeycloakUserId()));
            } catch (Exception e) {
                log.warn("Failed to get user roles from Keycloak: {}", e.getMessage());
                // Continue without roles
            }
        }

        // 3. Get tenant name from tenant service
        String tenantName = null;
        try {
            Optional<TenantServicePort.TenantInfo> tenantInfo = tenantServicePort.getTenantInfo(view.getTenantId());
            if (tenantInfo.isPresent()) {
                tenantName = tenantInfo.get().name();
                log.debug("Retrieved tenant name: tenantId={}, name={}", view.getTenantId().getValue(), tenantName);
            } else {
                log.warn("Tenant info not found for tenantId: {}", view.getTenantId().getValue());
            }
        } catch (Exception e) {
            log.warn("Failed to get tenant name from tenant service: {}", e.getMessage());
            // Continue without tenant name
        }

        // 4. Map view to query result
        return new GetUserQueryResult(view.getUserId(), view.getTenantId(), tenantName, view.getUsername(), view.getEmail(), view.getFirstName(), view.getLastName(),
                view.getStatus(), view.getKeycloakUserId(), roles, view.getCreatedAt(), view.getLastModifiedAt());
    }
}

