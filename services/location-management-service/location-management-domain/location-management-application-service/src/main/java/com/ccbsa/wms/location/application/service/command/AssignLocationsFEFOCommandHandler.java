package com.ccbsa.wms.location.application.service.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.BigDecimalQuantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.wms.location.application.service.command.dto.AssignLocationsFEFOCommand;
import com.ccbsa.wms.location.application.service.command.dto.AssignLocationsFEResult;
import com.ccbsa.wms.location.application.service.port.messaging.LocationEventPublisher;
import com.ccbsa.wms.location.application.service.port.repository.LocationRepository;
import com.ccbsa.wms.location.domain.core.entity.Location;
import com.ccbsa.wms.location.domain.core.exception.LocationNotFoundException;
import com.ccbsa.wms.location.domain.core.service.FEFOAssignmentService;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.StockItemAssignmentRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
@RequiredArgsConstructor
public class AssignLocationsFEFOCommandHandler {
    private final LocationRepository locationRepository;
    private final FEFOAssignmentService fefoAssignmentService;
    private final LocationEventPublisher eventPublisher;

    @Transactional
    public AssignLocationsFEResult handle(AssignLocationsFEFOCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Get available locations
        List<Location> availableLocations = locationRepository.findAvailableLocations(command.getTenantId());

        // Handle case where no locations are available
        // This is acceptable - stock items can remain unassigned and be assigned later
        // or allocated from unassigned items (handled in allocation logic)
        if (availableLocations.isEmpty()) {
            log.warn("No available locations found for FEFO assignment. Stock items will remain unassigned. "
                            + "tenantId={}, stockItemCount={}. Stock items can be assigned later when locations become available.", command.getTenantId().getValue(),
                    command.getStockItems().size());
            // Return empty assignments - stock items remain unassigned
            return AssignLocationsFEResult.builder().assignments(Collections.emptyMap()).build();
        }

        // 2a. Validate all locations are BIN type (stock allocation must be at lowest hierarchy level)
        List<Location> nonBinLocations = availableLocations.stream().filter(location -> {
            if (location.getType() == null || location.getType().getValue() == null) {
                return true; // Locations without type are considered non-BIN
            }
            return !"BIN".equalsIgnoreCase(location.getType().getValue().trim());
        }).collect(Collectors.toList());

        if (!nonBinLocations.isEmpty()) {
            String nonBinLocationIds = nonBinLocations.stream().map(loc -> loc.getId().getValueAsString()).collect(Collectors.joining(", "));
            throw new IllegalArgumentException(String.format("Stock allocation must be at BIN level (lowest hierarchy level). Found non-BIN locations: %s", nonBinLocationIds));
        }

        // 3. Assign locations using FEFO algorithm
        // Note: This may return partial assignments if some stock items cannot be assigned
        Map<String, LocationId> assignments = fefoAssignmentService.assignLocationsFEFO(command.getStockItems(), availableLocations);

        // Log if not all stock items were assigned
        if (assignments.size() < command.getStockItems().size()) {
            int unassignedCount = command.getStockItems().size() - assignments.size();
            log.warn("FEFO assignment completed with partial assignments. Assigned: {}, Unassigned: {}, tenantId={}. "
                    + "Unassigned stock items can be assigned later when locations become available.", assignments.size(), unassignedCount, command.getTenantId().getValue());
        }

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
            StockItemId stockItemIdValueObject = StockItemId.of(stockItemId);
            BigDecimalQuantity quantity = BigDecimalQuantity.of(stockItemRequest.getQuantity());
            location.assignStock(stockItemIdValueObject, quantity);

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
        // StockItems validation is done in DTO constructor - no need to check null again
        if (command.getStockItems().isEmpty()) {
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

