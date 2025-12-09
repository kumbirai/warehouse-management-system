package com.ccbsa.wms.tenant.application.service.command;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.exception.EntityNotFoundException;
import com.ccbsa.wms.tenant.application.service.command.dto.UpdateTenantConfigurationCommand;
import com.ccbsa.wms.tenant.application.service.port.messaging.TenantEventPublisher;
import com.ccbsa.wms.tenant.application.service.port.repository.TenantRepository;
import com.ccbsa.wms.tenant.domain.core.entity.Tenant;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Command Handler: UpdateTenantConfigurationCommandHandler
 * <p>
 * Handles tenant configuration updates.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Ports are managed singletons injected by Spring and kept immutable")
public class UpdateTenantConfigurationCommandHandler {
    private final TenantRepository tenantRepository;
    private final TenantEventPublisher eventPublisher;

    public UpdateTenantConfigurationCommandHandler(TenantRepository tenantRepository,
                                                   TenantEventPublisher eventPublisher) {
        this.tenantRepository = tenantRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void handle(UpdateTenantConfigurationCommand command) {
        // Find tenant
        Tenant tenant = tenantRepository.findById(command.getTenantId())
                .orElseThrow(() -> new EntityNotFoundException(String.format("Tenant not found: %s",
                        command.getTenantId())));

        // Update configuration (domain logic)
        tenant.updateConfiguration(command.getConfiguration());

        // Save tenant
        tenantRepository.save(tenant);

        // Publish domain events
        tenant.getDomainEvents()
                .forEach(eventPublisher::publish);
        tenant.clearDomainEvents();
    }
}

