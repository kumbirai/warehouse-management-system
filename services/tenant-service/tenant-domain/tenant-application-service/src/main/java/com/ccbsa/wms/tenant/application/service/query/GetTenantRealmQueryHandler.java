package com.ccbsa.wms.tenant.application.service.query;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.application.service.port.data.TenantViewRepository;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Query Handler: GetTenantRealmQueryHandler
 * <p>
 * Handles queries to get the Keycloak realm name for a tenant. Used by user-service to determine which realm to create users in.
 * <p>
 * Uses data port (TenantViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Ports are managed singletons injected by Spring and kept immutable")
public class GetTenantRealmQueryHandler {
    private final TenantViewRepository viewRepository;

    public GetTenantRealmQueryHandler(TenantViewRepository viewRepository) {
        this.viewRepository = viewRepository;
    }

    @Transactional(readOnly = true)
    public Optional<String> handle(TenantId tenantId) {
        return viewRepository.findById(tenantId).flatMap(view -> view.getKeycloakRealmName());
    }
}

