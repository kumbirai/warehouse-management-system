package com.ccbsa.wms.location.application.service.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
import com.ccbsa.wms.location.domain.core.exception.CodeAlreadyExistsException;
import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: CreateLocationCommandHandler
 * <p>
 * Handles creation of new Location aggregate.
 * <p>
 * Responsibilities: - Validates barcode uniqueness - Creates Location aggregate - Persists aggregate - Publishes domain events after transaction commit
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CreateLocationCommandHandler {
    private final LocationRepository repository;
    private final LocationEventPublisher eventPublisher;

    @Transactional
    public CreateLocationResult handle(CreateLocationCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Validate parent location exists if parentLocationId is provided
        if (command.getParentLocationId() != null && !command.getParentLocationId().trim().isEmpty()) {
            validateParentLocationExists(command.getParentLocationId(), command.getTenantId());
        }

        // 3. Validate barcode uniqueness if barcode is provided
        if (command.getBarcode() != null) {
            validateBarcodeUniqueness(command.getBarcode(), command.getTenantId());
        }

        // 4. Validate code uniqueness if code is provided
        if (command.getCode() != null && !command.getCode().trim().isEmpty()) {
            validateCodeUniqueness(command.getCode(), command.getTenantId());
        }

        // 4. Create aggregate using builder
        Location.Builder builder =
                Location.builder().locationId(LocationId.generate()).tenantId(command.getTenantId()).coordinates(command.getCoordinates()).status(LocationStatus.AVAILABLE);

        // Set barcode if provided, otherwise it will be auto-generated in build()
        if (command.getBarcode() != null) {
            builder.barcode(command.getBarcode());
        }

        // Set code, name, type if provided
        if (command.getCode() != null && !command.getCode().trim().isEmpty()) {
            builder.code(command.getCode());
        }
        if (command.getName() != null && !command.getName().trim().isEmpty()) {
            builder.name(command.getName());
        }
        if (command.getType() != null && !command.getType().trim().isEmpty()) {
            builder.type(command.getType());
        }

        // Set description if provided
        if (command.getDescription() != null && !command.getDescription().trim().isEmpty()) {
            builder.description(command.getDescription());
        }

        // Set parent location ID if provided
        if (command.getParentLocationId() != null && !command.getParentLocationId().trim().isEmpty()) {
            try {
                LocationId parentId = LocationId.of(UUID.fromString(command.getParentLocationId()));
                builder.parentLocationId(parentId);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(String.format("Invalid parent location ID format: %s", command.getParentLocationId()), e);
            }
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
        return CreateLocationResult.builder().locationId(savedLocation.getId()).barcode(savedLocation.getBarcode()).coordinates(savedLocation.getCoordinates())
                .status(savedLocation.getStatus()).createdAt(savedLocation.getCreatedAt()).code(savedLocation.getCode() != null ? savedLocation.getCode().getValue() : null)
                .name(savedLocation.getName() != null ? savedLocation.getName().getValue() : null).type(savedLocation.getType() != null ? savedLocation.getType().getValue() : null)
                .path(generatePath(savedLocation, command)).build();
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
        // Validate code is required for WAREHOUSE type
        String type = command.getType();
        if (type != null && "WAREHOUSE".equalsIgnoreCase(type.trim())) {
            if (command.getCode() == null || command.getCode().trim().isEmpty()) {
                throw new IllegalArgumentException("Code is required for WAREHOUSE type");
            }
        }
    }

    /**
     * Validates that the parent location exists.
     *
     * @param parentLocationId Parent location ID to validate
     * @param tenantId         Tenant identifier
     * @throws IllegalArgumentException if parent location does not exist (returns 400 BAD_REQUEST)
     */
    private void validateParentLocationExists(String parentLocationId, com.ccbsa.common.domain.valueobject.TenantId tenantId) {
        try {
            LocationId parentId = LocationId.of(UUID.fromString(parentLocationId));
            if (repository.findByIdAndTenantId(parentId, tenantId).isEmpty()) {
                throw new IllegalArgumentException(String.format("Parent location not found: %s", parentLocationId));
            }
        } catch (IllegalArgumentException e) {
            // Re-throw as-is - will be handled by GlobalExceptionHandler as 400 BAD_REQUEST
            throw e;
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
     * Validates that the code is unique for the tenant.
     *
     * @param code     Code to validate
     * @param tenantId Tenant identifier
     * @throws CodeAlreadyExistsException if code already exists
     */
    private void validateCodeUniqueness(String code, com.ccbsa.common.domain.valueobject.TenantId tenantId) {
        if (repository.existsByCodeAndTenantId(code, tenantId)) {
            throw new CodeAlreadyExistsException(String.format("Location code already exists: %s", code));
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

    /**
     * Generates a hierarchical path for the location by recursively traversing up the parent hierarchy.
     * For warehouses, returns "/{code}".
     * For child locations, returns "/{warehouseCode}/{zoneCode}/{aisleCode}/{rackCode}/{binCode}" by recursively loading parents.
     *
     * @param location Location aggregate (with stored parentLocationId)
     * @param command  Create location command (contains parentLocationId for initial creation)
     * @return Path string with full hierarchy
     */
    private String generatePath(Location location, CreateLocationCommand command) {
        String locationCode = location.getCode() != null ? location.getCode().getValue() : null;
        if (locationCode == null || locationCode.trim().isEmpty()) {
            locationCode = location.getBarcode().getValue();
        }

        // If this is a warehouse (no parent), return "/{code}"
        if (location.getParentLocationId() == null) {
            return String.format("/%s", locationCode);
        }

        // For child locations, recursively build path by traversing up the hierarchy using stored parentLocationId
        Set<LocationId> visitedIds = new HashSet<>();
        String parentPath = buildParentPathRecursively(location.getParentLocationId(), command.getTenantId(), visitedIds);
        // Build hierarchical path: /{parentPath}/{childCode}
        String hierarchicalPath = String.format("%s/%s", parentPath, locationCode);
        log.debug("Generated hierarchical path: {} for location with parent: {}", hierarchicalPath, location.getParentLocationId().getValueAsString());
        return hierarchicalPath;
    }

    /**
     * Recursively builds the parent path by traversing up the location hierarchy using stored parent_location_id.
     * This method traverses from the given parent location ID up to the warehouse (root of hierarchy).
     *
     * @param parentLocationId Parent location ID to start traversal from
     * @param tenantId         Tenant identifier
     * @param visitedIds       Set of visited location IDs to prevent infinite loops
     * @return Path string (e.g., "/WH-12/ZONE-X" for a zone)
     */
    private String buildParentPathRecursively(LocationId parentLocationId, com.ccbsa.common.domain.valueobject.TenantId tenantId, Set<LocationId> visitedIds) {
        // Prevent infinite loops
        if (visitedIds.contains(parentLocationId)) {
            log.warn("Circular reference detected in location hierarchy: {}", parentLocationId.getValueAsString());
            return "";
        }
        visitedIds.add(parentLocationId);

        Location parentLocation = repository.findByIdAndTenantId(parentLocationId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Parent location not found during path generation: %s", parentLocationId.getValueAsString())));

        String locationCode = parentLocation.getCode() != null ? parentLocation.getCode().getValue() : null;
        if (locationCode == null || locationCode.trim().isEmpty()) {
            locationCode = parentLocation.getBarcode().getValue();
        }

        // If this is a warehouse (type is WAREHOUSE or no parent), return "/{code}"
        String locationType = parentLocation.getType() != null ? parentLocation.getType().getValue() : null;
        if (locationType != null && "WAREHOUSE".equalsIgnoreCase(locationType.trim()) || parentLocation.getParentLocationId() == null) {
            return String.format("/%s", locationCode);
        }

        // Recursively build path for parent's parent
        String parentPath = buildParentPathRecursively(parentLocation.getParentLocationId(), tenantId, visitedIds);
        return String.format("%s/%s", parentPath, locationCode);
    }
}

