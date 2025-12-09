package com.ccbsa.wms.tenant.application.service.command;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.tenant.application.service.command.dto.CreateTenantCommand;
import com.ccbsa.wms.tenant.application.service.command.dto.CreateTenantResult;
import com.ccbsa.wms.tenant.application.service.port.messaging.TenantEventPublisher;
import com.ccbsa.wms.tenant.application.service.port.repository.TenantRepository;
import com.ccbsa.wms.tenant.domain.core.entity.Tenant;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Command Handler: CreateTenantCommandHandler
 * <p>
 * Handles the creation of a new tenant.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Ports are managed singletons injected by Spring and kept immutable")
public class CreateTenantCommandHandler {
    private final TenantRepository tenantRepository;
    private final TenantEventPublisher eventPublisher;

    public CreateTenantCommandHandler(TenantRepository tenantRepository,
                                      TenantEventPublisher eventPublisher) {
        this.tenantRepository = tenantRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CreateTenantResult handle(CreateTenantCommand command) {
        // Check if tenant already exists
        if (tenantRepository.existsById(command.getTenantId())) {
            return CreateTenantResult.failure(command.getTenantId(),
                    String.format("Tenant with ID %s already exists",
                            command.getTenantId()));
        }

        // Create tenant aggregate
        Tenant tenant = Tenant.builder()
                .tenantId(command.getTenantId())
                .name(command.getName())
                .contactInformation(command.getContactInformation())
                .configuration(command.getConfiguration())
                .build();

        // Save tenant
        tenantRepository.save(tenant);

        // Publish domain events
        tenant.getDomainEvents()
                .forEach(eventPublisher::publish);
        tenant.clearDomainEvents();

        return CreateTenantResult.success(tenant.getId());
    }
}

