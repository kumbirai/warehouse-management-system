package com.ccbsa.wms.location.application.service.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.location.application.service.command.dto.AssignLocationsFEFOCommand;
import com.ccbsa.wms.location.application.service.command.dto.AssignLocationsFEResult;
import com.ccbsa.wms.location.application.service.port.messaging.LocationEventPublisher;
import com.ccbsa.wms.location.application.service.port.repository.LocationRepository;
import com.ccbsa.wms.location.domain.core.entity.Location;
import com.ccbsa.wms.location.domain.core.exception.LocationNotFoundException;
import com.ccbsa.wms.location.domain.core.service.FEFOAssignmentService;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.StockItemAssignmentRequest;

/**
 * Command Handler: AssignLocationsFEFOCommandHandler
 * <p>
 * Handles FEFO location assignment to stock items.
 * <p>
 * Responsibilities:
 * - Get available locations
 * - Use FEFOAssignmentService to match stock items to locations
 * - Update location status and capacity
 * - Publish LocationAssignedEvent and LocationStatusChangedEvent
 */
@Component
public class AssignLocationsFEFOCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(AssignLocationsFEFOCommandHandler.class);

    private final LocationRepository locationRepository;
    private final FEFOAssignmentService fefoAssignmentService;
    private final LocationEventPublisher eventPublisher;

    public AssignLocationsFEFOCommandHandler(LocationRepository locationRepository, FEFOAssignmentService fefoAssignmentService, LocationEventPublisher eventPublisher) {
        this.locationRepository = locationRepository;
        this.fefoAssignmentService = fefoAssignmentService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AssignLocationsFEResult handle(AssignLocationsFEFOCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Get available locations
        List<Location> availableLocations = locationRepository.findAvailableLocations(command.getTenantId());

        if (availableLocations.isEmpty()) {
            throw new IllegalStateException("No available locations found for FEFO assignment");
        }

        // 3. Assign locations using FEFO algorithm
        Map<String, LocationId> assignments = fefoAssignmentService.assignLocationsFEFO(command.getStockItems(), availableLocations);

        // 4. Update locations and collect events
        List<DomainEvent<?>> allDomainEvents = new ArrayList<>();

        for (Map.Entry<String, LocationId> assignment : assignments.entrySet()) {
            String stockItemId = assignment.getKey();
            LocationId locationId = assignment.getValue();

            Location location = locationRepository.findByIdAndTenantId(locationId, command.getTenantId())
                    .orElseThrow(() -> new LocationNotFoundException(String.format("Location not found: %s", locationId.getValueAsString())));

            // Find the stock item request to get quantity
            StockItemAssignmentRequest stockItemRequest = command.getStockItems().stream().filter(si -> si.getStockItemId().equals(stockItemId)).findFirst()
                    .orElseThrow(() -> new IllegalStateException(String.format("Stock item request not found: %s", stockItemId)));

            // Assign stock to location
            location.assignStock(stockItemId, stockItemRequest.getQuantity());

            // Get domain events BEFORE saving
            List<DomainEvent<?>> locationEvents = new ArrayList<>(location.getDomainEvents());

            // Persist location
            locationRepository.save(location);

            // Collect events
            allDomainEvents.addAll(locationEvents);
        }

        // 5. Publish events after transaction commit
        if (!allDomainEvents.isEmpty()) {
            publishEventsAfterCommit(allDomainEvents);
        }

        // 6. Return result
        return AssignLocationsFEResult.builder().assignments(assignments).build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(AssignLocationsFEFOCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getStockItems() == null || command.getStockItems().isEmpty()) {
            throw new IllegalArgumentException("Stock items list cannot be empty");
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

