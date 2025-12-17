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
import com.ccbsa.wms.location.application.service.command.dto.CreateLocationCommand;
import com.ccbsa.wms.location.application.service.command.dto.CreateLocationResult;
import com.ccbsa.wms.location.application.service.port.messaging.LocationEventPublisher;
import com.ccbsa.wms.location.application.service.port.repository.LocationRepository;
import com.ccbsa.wms.location.domain.core.entity.Location;
import com.ccbsa.wms.location.domain.core.exception.BarcodeAlreadyExistsException;
import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

/**
 * Command Handler: CreateLocationCommandHandler
 * <p>
 * Handles creation of new Location aggregate.
 * <p>
 * Responsibilities: - Validates barcode uniqueness - Creates Location aggregate - Persists aggregate - Publishes domain events after transaction commit
 */
@Component
public class CreateLocationCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(CreateLocationCommandHandler.class);

    private final LocationRepository repository;
    private final LocationEventPublisher eventPublisher;

    public CreateLocationCommandHandler(LocationRepository repository, LocationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CreateLocationResult handle(CreateLocationCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Validate barcode uniqueness if barcode is provided
        if (command.getBarcode() != null) {
            validateBarcodeUniqueness(command.getBarcode(), command.getTenantId());
        }

        // 3. Create aggregate using builder
        Location.Builder builder = Location.builder()
                .locationId(LocationId.generate())
                .tenantId(command.getTenantId())
                .coordinates(command.getCoordinates())
                .status(LocationStatus.AVAILABLE);

        // Set barcode if provided, otherwise it will be auto-generated in build()
        if (command.getBarcode() != null) {
            builder.barcode(command.getBarcode());
        }

        // Set description if provided
        if (command.getDescription() != null && !command.getDescription()
                .trim()
                .isEmpty()) {
            builder.description(command.getDescription());
        }

        Location location = builder.build();

        // 4. Get domain events BEFORE saving (save() returns a new instance without events)
        // This is critical - domain events are added during build() and must be captured
        // before the repository save() which returns a new mapped instance
        List<DomainEvent<?>> domainEvents = new ArrayList<>(location.getDomainEvents());

        // 5. Persist aggregate (this returns a new instance from mapper, without domain events)
        Location savedLocation = repository.save(location);

        // 6. Publish events after transaction commit to avoid race conditions
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            // Clear events from original location (savedLocation doesn't have them)
            location.clearDomainEvents();
        }

        // 7. Return result (use savedLocation which has updated version from DB)
        return CreateLocationResult.builder()
                .locationId(savedLocation.getId())
                .barcode(savedLocation.getBarcode())
                .coordinates(savedLocation.getCoordinates())
                .status(savedLocation.getStatus())
                .createdAt(savedLocation.getCreatedAt())
                .build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(CreateLocationCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getCoordinates() == null) {
            throw new IllegalArgumentException("LocationCoordinates is required");
        }
    }

    /**
     * Validates that the barcode is unique for the tenant.
     *
     * @param barcode  Barcode to validate
     * @param tenantId Tenant identifier
     * @throws BarcodeAlreadyExistsException if barcode already exists
     */
    private void validateBarcodeUniqueness(LocationBarcode barcode, com.ccbsa.common.domain.valueobject.TenantId tenantId) {
        if (repository.existsByBarcodeAndTenantId(barcode, tenantId)) {
            throw new BarcodeAlreadyExistsException(String.format("Location barcode already exists: %s", barcode.getValue()));
        }
    }

    /**
     * Publishes domain events after transaction commit to avoid race conditions.
     * <p>
     * Events are published using TransactionSynchronizationManager to ensure they are only published after the database transaction has successfully committed. This prevents race
     * conditions where event listeners consume events before the
     * location is visible in the database.
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

