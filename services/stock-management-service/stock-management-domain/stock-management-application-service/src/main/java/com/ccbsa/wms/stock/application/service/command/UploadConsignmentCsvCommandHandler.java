package com.ccbsa.wms.stock.application.service.command;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.WarehouseId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.stock.application.service.command.dto.ConsignmentCsvError;
import com.ccbsa.wms.stock.application.service.command.dto.ConsignmentCsvRow;
import com.ccbsa.wms.stock.application.service.command.dto.UploadConsignmentCsvCommand;
import com.ccbsa.wms.stock.application.service.command.dto.UploadConsignmentCsvResult;
import com.ccbsa.wms.stock.application.service.port.messaging.StockManagementEventPublisher;
import com.ccbsa.wms.stock.application.service.port.repository.StockConsignmentRepository;
import com.ccbsa.wms.stock.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.stock.domain.core.entity.StockConsignment;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentLineItem;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentReference;

/**
 * Command Handler: UploadConsignmentCsvCommandHandler
 * <p>
 * Handles uploading consignment data via CSV file.
 * <p>
 * Responsibilities: - Parse CSV content - Validate each CSV row - Group rows by ConsignmentReference - Create consignments with line items - Collect and publish domain events -
 * Return upload result with statistics and errors
 */
@Component
public class UploadConsignmentCsvCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(UploadConsignmentCsvCommandHandler.class);

    private final StockConsignmentRepository repository;
    private final StockManagementEventPublisher eventPublisher;
    private final ProductServicePort productServicePort;
    private final ConsignmentCsvParser csvParser;

    public UploadConsignmentCsvCommandHandler(StockConsignmentRepository repository, StockManagementEventPublisher eventPublisher, ProductServicePort productServicePort,
                                              ConsignmentCsvParser csvParser) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.productServicePort = productServicePort;
        this.csvParser = csvParser;
    }

    @Transactional
    public UploadConsignmentCsvResult handle(UploadConsignmentCsvCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Parse CSV content
        List<ConsignmentCsvRow> rows;
        try {
            rows = csvParser.parse(command.getCsvInputStream());
        } catch (IOException e) {
            logger.error("Failed to parse CSV file: {}", e.getMessage());
            throw new IllegalArgumentException(String.format("Failed to parse CSV file: %s", e.getMessage()), e);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid CSV format: {}", e.getMessage());
            throw new IllegalArgumentException(String.format("Invalid CSV format: %s", e.getMessage()), e);
        }

        // 3. Group rows by ConsignmentReference
        Map<String, List<ConsignmentCsvRow>> rowsByConsignment = rows.stream().collect(Collectors.groupingBy(ConsignmentCsvRow::getConsignmentReference));

        // 4. Process each consignment
        int processedRows = 0;
        int createdConsignments = 0;
        int errorRows = 0;
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

                    // Collect domain events
                    List<DomainEvent<?>> consignmentEvents = new ArrayList<>(consignment.getDomainEvents());
                    allEvents.addAll(consignmentEvents);
                    consignment.clearDomainEvents();

                    createdConsignments++;
                    logger.debug("Created consignment from CSV: {}", consignmentRef);
                }

            } catch (RuntimeException e) {
                logger.warn("Error processing consignment {}: {}", consignmentRef, e.getMessage());
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

