package com.ccbsa.wms.tenant.application.service.command;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.exception.EntityNotFoundException;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.application.service.command.dto.ActivateTenantCommand;
import com.ccbsa.wms.tenant.application.service.port.messaging.TenantEventPublisher;
import com.ccbsa.wms.tenant.application.service.port.repository.TenantRepository;
import com.ccbsa.wms.tenant.application.service.port.service.KeycloakRealmServicePort;
import com.ccbsa.wms.tenant.application.service.port.service.TenantGroupServicePort;
import com.ccbsa.wms.tenant.domain.core.entity.Tenant;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: ActivateTenantCommandHandler
 * <p>
 * Handles tenant activation, including Keycloak realm creation/enablement.
 */
@Slf4j
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Ports are managed singletons injected by Spring and kept immutable")
@RequiredArgsConstructor
public class ActivateTenantCommandHandler {
    private final TenantRepository tenantRepository;
    private final TenantEventPublisher eventPublisher;
    private final KeycloakRealmServicePort keycloakRealmPort;
    private final TenantGroupServicePort tenantGroupServicePort;

    @Transactional
    public void handle(ActivateTenantCommand command) {
        // Find tenant
        Tenant tenant =
                tenantRepository.findById(command.getTenantId()).orElseThrow(() -> new EntityNotFoundException(String.format("Tenant not found: %s", command.getTenantId())));

        // Activate tenant (domain logic)
        tenant.activate();

        // Handle Keycloak realm creation/enablement if using per-tenant realms
        if (tenant.getConfiguration().isUsePerTenantRealm()) {
            String realmName = tenant.getConfiguration().getKeycloakRealmName().orElseGet(() -> generateRealmName(tenant.getId()));

            if (!keycloakRealmPort.realmExists(realmName)) {
                keycloakRealmPort.createRealm(tenant.getId(), realmName);
            } else {
                keycloakRealmPort.enableRealm(realmName);
            }

            // Update tenant configuration with realm name if not already set
            if (tenant.getConfiguration().getKeycloakRealmName().isEmpty()) {
                tenant.updateConfiguration(tenant.getConfiguration().withKeycloakRealmName(realmName));
            }
        } else {
            tenantGroupServicePort.ensureTenantGroupEnabled(tenant.getId());
        }
        // If using single realm, Keycloak group orchestration handles access

        // Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = List.copyOf(tenant.getDomainEvents());

        // Save tenant
        tenantRepository.save(tenant);

        // Publish domain events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            tenant.clearDomainEvents();
        }
    }

    private String generateRealmName(TenantId tenantId) {
        return String.format("tenant-%s", tenantId.getValue());
    }

    /**
     * Publishes domain events after transaction commit to avoid race conditions.
     * <p>
     * Events are published using TransactionSynchronizationManager to ensure they are only published after the database transaction has successfully committed. This prevents race
     * conditions where event listeners consume events before the tenant is visible in the database.
     *
     * @param domainEvents Domain events to publish
     */
    private void publishEventsAfterCommit(List<DomainEvent<?>> domainEvents) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // No active transaction - publish immediately
            log.debug("No active transaction - publishing events immediately");
            eventPublisher.publish(domainEvents);
            return;
        }

        // Register synchronization to publish events after transaction commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    log.debug("Transaction committed - publishing {} domain events", domainEvents.size());
                    eventPublisher.publish(domainEvents);
                } catch (Exception e) {
                    log.error("Failed to publish domain events after transaction commit", e);
                    // Don't throw - transaction already committed, event publishing failure
                    // should be handled by retry mechanisms or dead letter queue
                }
            }
        });
    }
}

