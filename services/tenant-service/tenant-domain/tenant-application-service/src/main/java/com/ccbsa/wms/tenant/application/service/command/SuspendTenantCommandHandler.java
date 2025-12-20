package com.ccbsa.wms.tenant.application.service.command;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.exception.EntityNotFoundException;
import com.ccbsa.wms.tenant.application.service.command.dto.SuspendTenantCommand;
import com.ccbsa.wms.tenant.application.service.port.messaging.TenantEventPublisher;
import com.ccbsa.wms.tenant.application.service.port.repository.TenantRepository;
import com.ccbsa.wms.tenant.application.service.port.service.KeycloakRealmServicePort;
import com.ccbsa.wms.tenant.application.service.port.service.TenantGroupServicePort;
import com.ccbsa.wms.tenant.domain.core.entity.Tenant;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Command Handler: SuspendTenantCommandHandler
 * <p>
 * Handles tenant suspension, including Keycloak realm disablement if configured.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Ports are managed singletons injected by Spring and kept immutable")
public class SuspendTenantCommandHandler {
    private final TenantRepository tenantRepository;
    private final TenantEventPublisher eventPublisher;
    private final KeycloakRealmServicePort keycloakRealmPort;
    private final TenantGroupServicePort tenantGroupServicePort;

    public SuspendTenantCommandHandler(TenantRepository tenantRepository, TenantEventPublisher eventPublisher, KeycloakRealmServicePort keycloakRealmPort,
                                       TenantGroupServicePort tenantGroupServicePort) {
        this.tenantRepository = tenantRepository;
        this.eventPublisher = eventPublisher;
        this.keycloakRealmPort = keycloakRealmPort;
        this.tenantGroupServicePort = tenantGroupServicePort;
    }

    @Transactional
    public void handle(SuspendTenantCommand command) {
        // Find tenant
        Tenant tenant =
                tenantRepository.findById(command.getTenantId()).orElseThrow(() -> new EntityNotFoundException(String.format("Tenant not found: %s", command.getTenantId())));

        // Suspend tenant (domain logic)
        tenant.suspend();

        // Handle Keycloak realm disablement if using per-tenant realms
        if (tenant.getConfiguration().isUsePerTenantRealm()) {
            tenant.getConfiguration().getKeycloakRealmName().ifPresent(realmName -> {
                if (keycloakRealmPort.realmExists(realmName)) {
                    keycloakRealmPort.disableRealm(realmName);
                }
            });
        } else {
            tenantGroupServicePort.disableTenantGroup(tenant.getId());
        }

        // Save tenant
        tenantRepository.save(tenant);

        // Publish domain events
        tenant.getDomainEvents().forEach(eventPublisher::publish);
        tenant.clearDomainEvents();
    }
}

