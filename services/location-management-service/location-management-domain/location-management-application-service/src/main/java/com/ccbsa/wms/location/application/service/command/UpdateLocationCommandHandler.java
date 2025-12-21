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
import com.ccbsa.wms.location.application.service.command.dto.UpdateLocationCommand;
import com.ccbsa.wms.location.application.service.command.dto.UpdateLocationResult;
import com.ccbsa.wms.location.application.service.port.messaging.LocationEventPublisher;
import com.ccbsa.wms.location.application.service.port.repository.LocationRepository;
import com.ccbsa.wms.location.domain.core.entity.Location;
import com.ccbsa.wms.location.domain.core.exception.BarcodeAlreadyExistsException;
import com.ccbsa.wms.location.domain.core.exception.LocationNotFoundException;
import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

/**
 * Command Handler: UpdateLocationCommandHandler
 * <p>
 * Handles updating an existing Location aggregate.
 * <p>
 * Responsibilities: - Loads existing location - Validates barcode uniqueness if barcode is being updated - Updates location fields - Persists aggregate - Publishes domain events
 * after transaction commit
 */
@Component
public class UpdateLocationCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(UpdateLocationCommandHandler.class);

    private final LocationRepository repository;
    private final LocationEventPublisher eventPublisher;

    public UpdateLocationCommandHandler(LocationRepository repository, LocationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UpdateLocationResult handle(UpdateLocationCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Load existing location
        Location location = repository.findByIdAndTenantId(command.getLocationId(), command.getTenantId())
                .orElseThrow(() -> new LocationNotFoundException(String.format("Location not found: %s", command.getLocationId().getValueAsString())));

        // 3. Validate barcode uniqueness if barcode is being updated
        if (command.getBarcode() != null) {
            validateBarcodeUniqueness(command.getBarcode(), command.getTenantId(), command.getLocationId());
        }

        // 4. Update location fields using domain methods
        location.updateCoordinates(command.getCoordinates());

        if (command.getBarcode() != null) {
            location.updateBarcode(command.getBarcode());
        }

        if (command.getDescription() != null) {
            location.updateDescription(command.getDescription());
        }

        // 5. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = new ArrayList<>(location.getDomainEvents());

        // 6. Persist aggregate
        Location savedLocation = repository.save(location);

        // 7. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            location.clearDomainEvents();
        }

        // 8. Return result
        return UpdateLocationResult.builder().locationId(savedLocation.getId()).barcode(savedLocation.getBarcode()).coordinates(savedLocation.getCoordinates())
                .status(savedLocation.getStatus()).description(savedLocation.getDescription()).lastModifiedAt(savedLocation.getLastModifiedAt()).build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(UpdateLocationCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getLocationId() == null) {
            throw new IllegalArgumentException("LocationId is required");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getCoordinates() == null) {
            throw new IllegalArgumentException("LocationCoordinates is required");
        }
    }

    /**
     * Validates that the barcode is unique for the tenant (excluding the current location).
     *
     * @param barcode    Barcode to validate
     * @param tenantId   Tenant identifier
     * @param locationId Current location ID (to exclude from uniqueness check)
     * @throws BarcodeAlreadyExistsException if barcode already exists for another location
     */
    private void validateBarcodeUniqueness(LocationBarcode barcode, com.ccbsa.common.domain.valueobject.TenantId tenantId, LocationId locationId) {
        // Check if barcode exists for another location
        repository.findByBarcodeAndTenantId(barcode, tenantId).ifPresent(existingLocation -> {
            if (!existingLocation.getId().equals(locationId)) {
                throw new BarcodeAlreadyExistsException(String.format("Location barcode already exists: %s", barcode.getValue()));
            }
        });
    }

    /**
     * Publishes domain events after transaction commit to avoid race conditions.
     *
     * @param domainEvents Domain events to publish
     */
    private void publishEventsAfterCommit(List<DomainEvent<?>> domainEvents) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // No active transaction - publish immediately
            logger.debug("No active transaction - publishing events immediately");
            eventPublisher.publish(domainEvents);
            return;
        }

        // Register synchronization to publish events after transaction commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    logger.debug("Transaction committed - publishing {} domain events", domainEvents.size());
                    eventPublisher.publish(domainEvents);
                } catch (Exception e) {
                    logger.error("Failed to publish domain events after transaction commit", e);
                    // Don't throw - transaction already committed, event publishing failure
                    // should be handled by retry mechanisms or dead letter queue
                }
            }
        });
    }
}

