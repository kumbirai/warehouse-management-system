package com.ccbsa.wms.location.application.service.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.BigDecimalQuantity;
import com.ccbsa.common.domain.valueobject.ProductCondition;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.ReturnId;
import com.ccbsa.common.domain.valueobject.ReturnStatus;
import com.ccbsa.wms.location.application.service.command.dto.AssignReturnLocationsCommand;
import com.ccbsa.wms.location.application.service.command.dto.AssignReturnLocationsResult;
import com.ccbsa.wms.location.application.service.port.messaging.LocationEventPublisher;
import com.ccbsa.wms.location.application.service.port.repository.LocationRepository;
import com.ccbsa.wms.location.application.service.port.service.ReturnServicePort;
import com.ccbsa.wms.location.domain.core.entity.Location;
import com.ccbsa.wms.location.domain.core.event.ReturnLocationAssignedEvent;
import com.ccbsa.wms.location.domain.core.service.ReturnLocationAssignmentStrategy;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: AssignReturnLocationsCommandHandler
 * <p>
 * Handles location assignment to return line items based on product condition.
 * <p>
 * Responsibilities:
 * - Load return details via ReturnServicePort
 * - Apply ReturnLocationAssignmentStrategy to assign locations
 * - Update location status and capacity
 * - Publish ReturnLocationAssignedEvent
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AssignReturnLocationsCommandHandler {
    private final ReturnServicePort returnServicePort;
    private final LocationRepository locationRepository;
    private final ReturnLocationAssignmentStrategy assignmentStrategy;
    private final LocationEventPublisher eventPublisher;

    @Transactional
    public AssignReturnLocationsResult handle(AssignReturnLocationsCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Load return details
        ReturnServicePort.ReturnDetails returnDetails = returnServicePort.getReturnDetails(command.getReturnId(), command.getTenantId())
                .orElseThrow(() -> new IllegalStateException("Return not found: " + command.getReturnId().getValueAsString()));

        // 3. Validate return is in correct status
        if (returnDetails.status() != ReturnStatus.PROCESSED) {
            throw new IllegalStateException("Return must be in PROCESSED status for location assignment. Current status: " + returnDetails.status());
        }

        // 4. Get available locations
        List<Location> availableLocations = locationRepository.findAvailableLocations(command.getTenantId());

        if (availableLocations.isEmpty()) {
            log.warn("No available locations found for return location assignment. returnId={}, tenantId={}", command.getReturnId().getValueAsString(),
                    command.getTenantId().getValue());
            return AssignReturnLocationsResult.builder().assignments(new HashMap<>()).build();
        }

        // 5. Assign locations using strategy
        Map<String, LocationId> assignments = new HashMap<>();
        List<DomainEvent<?>> allDomainEvents = new ArrayList<>();

        for (ReturnServicePort.ReturnLineItemDetails lineItem : returnDetails.lineItems()) {
            try {
                ProductCondition condition = lineItem.productCondition();
                if (condition == null) {
                    log.warn("Skipping line item without product condition: lineItemId={}, returnId={}", lineItem.lineItemId().getValueAsString(),
                            command.getReturnId().getValueAsString());
                    continue;
                }

                // Assign location using strategy
                LocationId assignedLocationId = assignmentStrategy.assignLocationByCondition(lineItem.productId(), condition, lineItem.returnedQuantity(), availableLocations);

                assignments.put(lineItem.lineItemId().getValueAsString(), assignedLocationId);

                // Note: For returns, we don't update location capacity via assignStock()
                // because returns don't have stock items. The location assignment is tracked
                // in the Returns Service. The Location Management Service provides the
                // assignment logic, but the actual tracking is handled by the Returns Service.
                // If capacity tracking is needed for returns, a separate method would be
                // required in the Location entity.

                log.info("Assigned location to return line item: returnId={}, lineItemId={}, locationId={}, condition={}", command.getReturnId().getValueAsString(),
                        lineItem.lineItemId().getValueAsString(), assignedLocationId.getValueAsString(), condition);
            } catch (Exception e) {
                log.error("Failed to assign location to return line item: returnId={}, lineItemId={}, error={}", command.getReturnId().getValueAsString(),
                        lineItem.lineItemId().getValueAsString(), e.getMessage(), e);
                // Continue processing other line items
            }
        }

        // 6. Publish ReturnLocationAssignedEvent
        if (!assignments.isEmpty()) {
            // Use first assigned location as primary location ID for event
            LocationId primaryLocationId = assignments.values().iterator().next();
            ReturnLocationAssignedEvent event = new ReturnLocationAssignedEvent(command.getReturnId(), command.getTenantId(), assignments, primaryLocationId);
            allDomainEvents.add(event);
        }

        // 7. Publish events after transaction commit
        if (!allDomainEvents.isEmpty()) {
            publishEventsAfterCommit(allDomainEvents);
        }

        // 8. Return result
        return AssignReturnLocationsResult.builder().assignments(assignments).build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(AssignReturnLocationsCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getReturnId() == null) {
            throw new IllegalArgumentException("ReturnId is required");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
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
