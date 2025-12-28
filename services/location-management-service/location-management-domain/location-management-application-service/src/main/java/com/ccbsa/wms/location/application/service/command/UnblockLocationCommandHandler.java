package com.ccbsa.wms.location.application.service.command;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.location.application.service.command.dto.UnblockLocationCommand;
import com.ccbsa.wms.location.application.service.command.dto.UnblockLocationResult;
import com.ccbsa.wms.location.application.service.port.messaging.LocationEventPublisher;
import com.ccbsa.wms.location.application.service.port.repository.LocationRepository;
import com.ccbsa.wms.location.domain.core.entity.Location;
import com.ccbsa.wms.location.domain.core.exception.LocationNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: UnblockLocationCommandHandler
 * <p>
 * Handles unblocking of a Location aggregate.
 * <p>
 * Responsibilities:
 * - Loads Location aggregate
 * - Executes business logic via aggregate (unblock method)
 * - Persists aggregate changes
 * - Publishes domain events after transaction commit
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UnblockLocationCommandHandler {
    private final LocationRepository repository;
    private final LocationEventPublisher eventPublisher;

    @Transactional
    public UnblockLocationResult handle(UnblockLocationCommand command) {
        log.debug("Handling UnblockLocationCommand for location: {}", command.getLocationId());

        // 1. Validate command
        validateCommand(command);

        // 2. Load aggregate
        Location location = repository.findByIdAndTenantId(command.getLocationId(), command.getTenantId())
                .orElseThrow(() -> new LocationNotFoundException("Location not found: " + command.getLocationId().getValueAsString()));

        // 3. Execute business logic
        location.unblock(command.getUnblockedBy());

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
        return UnblockLocationResult.builder().locationId(savedLocation.getId()).status(savedLocation.getStatus()).lastModifiedAt(savedLocation.getLastModifiedAt()).build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(UnblockLocationCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getLocationId() == null) {
            throw new IllegalArgumentException("LocationId is required");
        }
        if (command.getUnblockedBy() == null) {
            throw new IllegalArgumentException("UnblockedBy is required");
        }
    }

    /**
     * Publishes domain events after transaction commit to avoid race conditions.
     *
     * @param domainEvents Domain events to publish
     */
    private void publishEventsAfterCommit(List<DomainEvent<?>> domainEvents) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            log.debug("No active transaction - publishing events immediately");
            eventPublisher.publish(domainEvents);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    log.debug("Transaction committed - publishing {} domain events", domainEvents.size());
                    eventPublisher.publish(domainEvents);
                } catch (Exception e) {
                    log.error("Failed to publish domain events after transaction commit", e);
                }
            }
        });
    }
}

