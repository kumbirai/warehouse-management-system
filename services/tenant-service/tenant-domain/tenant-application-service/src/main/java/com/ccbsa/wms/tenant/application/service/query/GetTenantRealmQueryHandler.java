package com.ccbsa.wms.tenant.application.service.query;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.application.service.port.repository.TenantRepository;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Query Handler: GetTenantRealmQueryHandler
 * <p>
 * Handles queries to get the Keycloak realm name for a tenant. Used by user-service to determine which realm to create users in.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Ports are managed singletons injected by Spring and kept immutable")
public class GetTenantRealmQueryHandler {
    private final TenantRepository tenantRepository;

    public GetTenantRealmQueryHandler(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public Optional<String> handle(TenantId tenantId) {
        return tenantRepository.findById(tenantId).flatMap(tenant -> tenant.getConfiguration().getKeycloakRealmName());
    }
}

