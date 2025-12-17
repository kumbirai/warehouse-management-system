package com.ccbsa.wms.tenant.application.service.command;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.exception.EntityNotFoundException;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.application.service.command.dto.ActivateTenantCommand;
import com.ccbsa.wms.tenant.application.service.port.messaging.TenantEventPublisher;
import com.ccbsa.wms.tenant.application.service.port.repository.TenantRepository;
import com.ccbsa.wms.tenant.application.service.port.service.KeycloakRealmServicePort;
import com.ccbsa.wms.tenant.application.service.port.service.TenantGroupServicePort;
import com.ccbsa.wms.tenant.domain.core.entity.Tenant;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Command Handler: ActivateTenantCommandHandler
 * <p>
 * Handles tenant activation, including Keycloak realm creation/enablement.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Ports are managed singletons injected by Spring and kept immutable")
public class ActivateTenantCommandHandler {
    private final TenantRepository tenantRepository;
    private final TenantEventPublisher eventPublisher;
    private final KeycloakRealmServicePort keycloakRealmPort;
    private final TenantGroupServicePort tenantGroupServicePort;

    public ActivateTenantCommandHandler(TenantRepository tenantRepository, TenantEventPublisher eventPublisher, KeycloakRealmServicePort keycloakRealmPort,
                                        TenantGroupServicePort tenantGroupServicePort) {
        this.tenantRepository = tenantRepository;
        this.eventPublisher = eventPublisher;
        this.keycloakRealmPort = keycloakRealmPort;
        this.tenantGroupServicePort = tenantGroupServicePort;
    }

    @Transactional
    public void handle(ActivateTenantCommand command) {
        // Find tenant
        Tenant tenant = tenantRepository.findById(command.getTenantId())
                .orElseThrow(() -> new EntityNotFoundException(String.format("Tenant not found: %s", command.getTenantId())));

        // Activate tenant (domain logic)
        tenant.activate();

        // Handle Keycloak realm creation/enablement if using per-tenant realms
        if (tenant.getConfiguration()
                .isUsePerTenantRealm()) {
            String realmName = tenant.getConfiguration()
                    .getKeycloakRealmName()
                    .orElseGet(() -> generateRealmName(tenant.getId()));

            if (!keycloakRealmPort.realmExists(realmName)) {
                keycloakRealmPort.createRealm(tenant.getId(), realmName);
            } else {
                keycloakRealmPort.enableRealm(realmName);
            }

            // Update tenant configuration with realm name if not already set
            if (tenant.getConfiguration()
                    .getKeycloakRealmName()
                    .isEmpty()) {
                tenant.updateConfiguration(tenant.getConfiguration()
                        .withKeycloakRealmName(realmName));
            }
        } else {
            tenantGroupServicePort.ensureTenantGroupEnabled(tenant.getId());
        }
        // If using single realm, Keycloak group orchestration handles access

        // Save tenant
        tenantRepository.save(tenant);

        // Publish domain events
        tenant.getDomainEvents()
                .forEach(eventPublisher::publish);
        tenant.clearDomainEvents();
    }

    private String generateRealmName(TenantId tenantId) {
        return String.format("tenant-%s", tenantId.getValue());
    }
}

