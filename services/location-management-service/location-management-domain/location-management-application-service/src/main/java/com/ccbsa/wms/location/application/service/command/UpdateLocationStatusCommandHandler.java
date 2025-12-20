package com.ccbsa.wms.location.application.service.command;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.location.application.service.command.dto.UpdateLocationStatusCommand;
import com.ccbsa.wms.location.application.service.command.dto.UpdateLocationStatusResult;
import com.ccbsa.wms.location.application.service.port.messaging.LocationEventPublisher;
import com.ccbsa.wms.location.application.service.port.repository.LocationRepository;
import com.ccbsa.wms.location.domain.core.entity.Location;
import com.ccbsa.wms.location.domain.core.exception.LocationNotFoundException;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

/**
 * Command Handler: UpdateLocationStatusCommandHandler
 * <p>
 * Handles updating a location's status.
 * <p>
 * Responsibilities: - Loads existing location - Updates location status - Persists aggregate - Publishes domain events after transaction commit
 */
@Component
public class UpdateLocationStatusCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(UpdateLocationStatusCommandHandler.class);

    private final LocationRepository repository;
    private final LocationEventPublisher eventPublisher;

    public UpdateLocationStatusCommandHandler(LocationRepository repository, LocationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UpdateLocationStatusResult handle(UpdateLocationStatusCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Load existing location
        Location location = repository.findByIdAndTenantId(command.getLocationId(), command.getTenantId())
                .orElseThrow(() -> new LocationNotFoundException(String.format("Location not found: %s", command.getLocationId().getValueAsString())));

        // 3. Update status
        LocationStatus newStatus = command.getStatus();
        location.updateStatus(newStatus);

        // 4. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = new ArrayList<>(location.getDomainEvents());

        // 5. Persist aggregate
        Location savedLocation = repository.save(location);

        // 6. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            location.clearDomainEvents();
        }

        // 7. Return result
        return UpdateLocationStatusResult.builder().locationId(savedLocation.getId()).status(savedLocation.getStatus().name()).lastModifiedAt(savedLocation.getLastModifiedAt())
                .build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(UpdateLocationStatusCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getLocationId() == null) {
            throw new IllegalArgumentException("LocationId is required");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getStatus() == null) {
            throw new IllegalArgumentException("Status is required");
        }
    }

    /**
     * Publishes domain events after transaction commit to avoid race conditions.
     *
     * @param domainEvents Domain events to publish
     */
    private void publishEventsAfterCommit(List<DomainEvent<?>> domainEvents) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            logger.debug("No active transaction - publishing events immediately");
            eventPublisher.publish(domainEvents);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    logger.debug("Transaction committed - publishing {} domain events", domainEvents.size());
                    eventPublisher.publish(domainEvents);
                } catch (Exception e) {
                    logger.error("Failed to publish domain events after transaction commit", e);
                }
            }
        });
    }
}

