package com.ccbsa.wms.stock.application.service.command;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.WarehouseId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.stock.application.service.command.dto.ConsignmentCsvError;
import com.ccbsa.wms.stock.application.service.command.dto.ConsignmentCsvRow;
import com.ccbsa.wms.stock.application.service.command.dto.UploadConsignmentCsvCommand;
import com.ccbsa.wms.stock.application.service.command.dto.UploadConsignmentCsvResult;
import com.ccbsa.wms.stock.application.service.port.messaging.StockManagementEventPublisher;
import com.ccbsa.wms.stock.application.service.port.repository.StockConsignmentRepository;
import com.ccbsa.wms.stock.application.service.port.service.LocationServicePort;
import com.ccbsa.wms.stock.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.stock.domain.core.entity.StockConsignment;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentLineItem;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentReference;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: UploadConsignmentCsvCommandHandler
 * <p>
 * Handles uploading consignment data via CSV file.
 * <p>
 * Responsibilities: - Parse CSV content - Validate each CSV row - Group rows by ConsignmentReference - Create consignments with line items - Collect and publish domain events -
 * Return upload result with statistics and errors
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UploadConsignmentCsvCommandHandler {
    private final StockConsignmentRepository repository;
    private final StockManagementEventPublisher eventPublisher;
    private final ProductServicePort productServicePort;
    private final LocationServicePort locationServicePort;
    private final ConsignmentCsvParser csvParser;

    @Transactional
    public UploadConsignmentCsvResult handle(UploadConsignmentCsvCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Parse CSV content
        List<ConsignmentCsvRow> rows;
        try {
            rows = csvParser.parse(command.getCsvInputStream());
        } catch (IOException e) {
            log.error("Failed to parse CSV file: {}", e.getMessage());
            throw new IllegalArgumentException(String.format("Failed to parse CSV file: %s", e.getMessage()), e);
        } catch (IllegalArgumentException e) {
            log.error("Invalid CSV format: {}", e.getMessage());
            throw new IllegalArgumentException(String.format("Invalid CSV format: %s", e.getMessage()), e);
        }

        // 3. Group rows by ConsignmentReference
        Map<String, List<ConsignmentCsvRow>> rowsByConsignment = rows.stream().collect(Collectors.groupingBy(ConsignmentCsvRow::getConsignmentReference));

        // 4. Process each consignment
        int processedRows = 0;
        int createdConsignments = 0;
        int errorRows = 0;
        // Errors list is populated during processing and returned in result
        @SuppressFBWarnings(value = "UC_USELESS_OBJECT", justification = "errors list is populated in loop and returned in result - SpotBugs false positive")
        List<ConsignmentCsvError> errors = new ArrayList<>();
        List<DomainEvent<?>> allEvents = new ArrayList<>();

        for (Map.Entry<String, List<ConsignmentCsvRow>> entry : rowsByConsignment.entrySet()) {
            String consignmentRef = entry.getKey();
            List<ConsignmentCsvRow> consignmentRows = entry.getValue();

            try {
                // Validate consignment reference uniqueness
                ConsignmentReference reference = ConsignmentReference.of(consignmentRef);
                if (repository.existsByConsignmentReferenceAndTenantId(reference, command.getTenantId())) {
                    // Add error for all rows in this consignment
                    for (ConsignmentCsvRow row : consignmentRows) {
                        errors.add(ConsignmentCsvError.builder().rowNumber(row.getRowNumber()).consignmentReference(consignmentRef).productCode(row.getProductCode())
                                .errorMessage("Consignment reference already exists").build());
                        errorRows++;
                    }
                    continue;
                }

                // Get warehouse ID from first row (all rows should have same warehouse)
                String warehouseIdStr = consignmentRows.get(0).getWarehouseId();
                WarehouseId warehouseId = WarehouseId.of(warehouseIdStr);

                // Validate warehouse location exists
                try {
                    LocationId locationId = LocationId.of(warehouseIdStr);
                    LocationServicePort.LocationAvailability availability = locationServicePort.checkLocationAvailability(locationId, Quantity.of(1), command.getTenantId());
                    if (!availability.isAvailable()) {
                        // Add error for all rows in this consignment
                        String errorMsg =
                                "Warehouse location not found or unavailable: " + (availability.getReason() != null ? availability.getReason() : "Location does not exist");
                        for (ConsignmentCsvRow row : consignmentRows) {
                            errors.add(ConsignmentCsvError.builder().rowNumber(row.getRowNumber()).consignmentReference(consignmentRef).productCode(row.getProductCode())
                                    .errorMessage(errorMsg).build());
                            errorRows++;
                        }
                        continue;
                    }
                } catch (Exception e) {
                    log.warn("Error validating warehouse location {}: {}", warehouseIdStr, e.getMessage());
                    // Add error for all rows in this consignment
                    for (ConsignmentCsvRow row : consignmentRows) {
                        errors.add(ConsignmentCsvError.builder().rowNumber(row.getRowNumber()).consignmentReference(consignmentRef).productCode(row.getProductCode())
                                .errorMessage("Warehouse location validation failed: " + e.getMessage()).build());
                        errorRows++;
                    }
                    continue;
                }

                // Get received date from first row
                LocalDateTime receivedAt = consignmentRows.get(0).getReceivedDate();

                // Convert CSV rows to line items
                List<ConsignmentLineItem> lineItems = new ArrayList<>();
                for (ConsignmentCsvRow row : consignmentRows) {
                    // Validate product code exists
                    ProductCode productCode = ProductCode.of(row.getProductCode());
                    var productInfo = productServicePort.getProductByCode(productCode, command.getTenantId());
                    if (productInfo.isEmpty()) {
                        errors.add(ConsignmentCsvError.builder().rowNumber(row.getRowNumber()).consignmentReference(consignmentRef).productCode(row.getProductCode())
                                .errorMessage(String.format("Product code not found: %s", row.getProductCode())).build());
                        errorRows++;
                        continue;
                    }

                    // Create line item
                    ConsignmentLineItem lineItem =
                            ConsignmentLineItem.builder().productCode(productCode).quantity(row.getQuantity()).expirationDate(row.getExpirationDate()).build();

                    lineItems.add(lineItem);
                    processedRows++;
                }

                // Only create consignment if we have at least one valid line item
                if (!lineItems.isEmpty()) {
                    // Create consignment
                    StockConsignment consignment =
                            StockConsignment.builder().consignmentId(ConsignmentId.generate()).tenantId(command.getTenantId()).consignmentReference(reference)
                                    .warehouseId(warehouseId).receivedAt(receivedAt).receivedBy(command.getReceivedBy()).lineItems(lineItems).build();

                    // Persist consignment
                    repository.save(consignment);

                    // Collect domain events from creation
                    List<DomainEvent<?>> consignmentEvents = new ArrayList<>(consignment.getDomainEvents());
                    allEvents.addAll(consignmentEvents);
                    consignment.clearDomainEvents();

                    // Auto-confirm consignment (CSV upload represents goods already received and verified)
                    // This triggers StockConsignmentConfirmedEvent which creates stock items
                    consignment.confirm();
                    repository.save(consignment);

                    // Collect domain events from confirmation
                    List<DomainEvent<?>> confirmationEvents = new ArrayList<>(consignment.getDomainEvents());
                    allEvents.addAll(confirmationEvents);
                    consignment.clearDomainEvents();

                    createdConsignments++;
                    log.debug("Created and confirmed consignment from CSV: {}", consignmentRef);
                }

            } catch (RuntimeException e) {
                log.warn("Error processing consignment {}: {}", consignmentRef, e.getMessage());
                // Add error for all rows in this consignment
                for (ConsignmentCsvRow row : consignmentRows) {
                    errors.add(ConsignmentCsvError.builder().rowNumber(row.getRowNumber()).consignmentReference(consignmentRef).productCode(row.getProductCode())
                            .errorMessage(e.getMessage()).build());
                    errorRows++;
                }
            }
        }

        // 5. Publish all events after transaction commit
        if (!allEvents.isEmpty()) {
            publishEventsAfterCommit(allEvents);
        }

        // 6. Build and return result
        return UploadConsignmentCsvResult.builder().totalRows(rows.size()).processedRows(processedRows).createdConsignments(createdConsignments).errorRows(errorRows).errors(errors)
                .build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(UploadConsignmentCsvCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getCsvInputStream() == null) {
            throw new IllegalArgumentException("CSV input stream is required");
        }
    }

    /**
     * Publishes domain events after transaction commit to avoid race conditions.
     * <p>
     * Events are published using TransactionSynchronizationManager to ensure they are only published after the database transaction has successfully committed. This prevents race
     * conditions where event listeners consume events before the aggregate is visible in the database.
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
}

