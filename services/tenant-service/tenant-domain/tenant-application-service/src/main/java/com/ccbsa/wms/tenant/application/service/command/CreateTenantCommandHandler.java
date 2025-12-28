package com.ccbsa.wms.tenant.application.service.command;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.tenant.application.service.command.dto.CreateTenantCommand;
import com.ccbsa.wms.tenant.application.service.command.dto.CreateTenantResult;
import com.ccbsa.wms.tenant.application.service.port.messaging.TenantEventPublisher;
import com.ccbsa.wms.tenant.application.service.port.repository.TenantRepository;
import com.ccbsa.wms.tenant.domain.core.entity.Tenant;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: CreateTenantCommandHandler
 * <p>
 * Handles the creation of a new tenant.
 */
@Slf4j
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Ports are managed singletons injected by Spring and kept immutable")
@RequiredArgsConstructor
public class CreateTenantCommandHandler {
    private final TenantRepository tenantRepository;
    private final TenantEventPublisher eventPublisher;

    @Transactional
    public CreateTenantResult handle(CreateTenantCommand command) {
        // Check if tenant already exists
        if (tenantRepository.existsById(command.getTenantId())) {
            return CreateTenantResult.failure(command.getTenantId(), String.format("Tenant with ID %s already exists", command.getTenantId()));
        }

        // Create tenant aggregate
        Tenant tenant = Tenant.builder().tenantId(command.getTenantId()).name(command.getName()).contactInformation(command.getContactInformation())
                .configuration(command.getConfiguration()).build();

        // Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = List.copyOf(tenant.getDomainEvents());

        // Save tenant
        tenantRepository.save(tenant);

        // Publish domain events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            tenant.clearDomainEvents();
        }

        return CreateTenantResult.success(tenant.getId());
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

