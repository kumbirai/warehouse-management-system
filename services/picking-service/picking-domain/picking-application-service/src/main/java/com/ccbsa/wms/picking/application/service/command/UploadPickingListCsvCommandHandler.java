package com.ccbsa.wms.picking.application.service.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.ccbsa.common.domain.valueobject.CustomerInfo;
import com.ccbsa.common.domain.valueobject.LoadNumber;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.Priority;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.picking.application.service.command.dto.CsvUploadResult;
import com.ccbsa.wms.picking.application.service.command.dto.PickingListCsvRow;
import com.ccbsa.wms.picking.application.service.command.dto.UploadPickingListCsvCommand;
import com.ccbsa.wms.picking.application.service.port.messaging.PickingEventPublisher;
import com.ccbsa.wms.picking.application.service.port.repository.PickingListReferenceGenerator;
import com.ccbsa.wms.picking.application.service.port.repository.PickingListRepository;
import com.ccbsa.wms.picking.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.picking.domain.core.entity.Load;
import com.ccbsa.wms.picking.domain.core.entity.Order;
import com.ccbsa.wms.picking.domain.core.entity.OrderLineItem;
import com.ccbsa.wms.picking.domain.core.entity.PickingList;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.Notes;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderId;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderLineItemId;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderStatus;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import com.ccbsa.wms.picking.domain.core.valueobject.ProductCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: UploadPickingListCsvCommandHandler
 * <p>
 * Handles CSV file upload for picking list creation.
 * <p>
 * Responsibilities:
 * - Parse CSV content
 * - Validate products against Product Service
 * - Create PickingList aggregates with Loads and Orders
 * - Publish domain events
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UploadPickingListCsvCommandHandler {
    private final PickingListCsvParser csvParser;
    private final PickingListRepository pickingListRepository;
    private final ProductServicePort productServicePort;
    private final PickingEventPublisher eventPublisher;
    private final PickingListReferenceGenerator referenceGenerator;

    @Transactional
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Catching all exceptions is intentional for batch CSV processing - continue processing other rows even if "
            + "one fails")
    public CsvUploadResult handle(UploadPickingListCsvCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Parse CSV content
        List<PickingListCsvRow> rows;
        try {
            rows = csvParser.parse(command.getCsvContent());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid CSV format: {}", e.getMessage());
            // When CSV format is invalid (e.g., missing headers), treat it as 1 error row
            return CsvUploadResult.builder().totalRows(1).successfulRows(0).errorRows(1).createdPickingListIds(List.of())
                    .errors(List.of(CsvUploadResult.CsvValidationError.builder().rowNumber(0).fieldName("CSV").errorMessage(e.getMessage()).invalidValue(null).build())).build();
        }

        // 3. Group rows by LoadNumber
        Map<String, List<PickingListCsvRow>> rowsByLoad = rows.stream().collect(Collectors.groupingBy(PickingListCsvRow::getLoadNumber));

        // 4. Process each load
        int totalRows = rows.size();
        int successfulRows = 0;
        int errorRows = 0;
        List<PickingListId> createdPickingListIds = new ArrayList<>();
        List<CsvUploadResult.CsvValidationError> errors = new ArrayList<>();
        List<DomainEvent<?>> allEvents = new ArrayList<>();

        for (Map.Entry<String, List<PickingListCsvRow>> entry : rowsByLoad.entrySet()) {
            String loadNumberStr = entry.getKey();
            List<PickingListCsvRow> loadRows = entry.getValue();

            try {
                // Group by OrderNumber within load
                Map<String, List<PickingListCsvRow>> rowsByOrder = loadRows.stream().collect(Collectors.groupingBy(PickingListCsvRow::getOrderNumber));

                // Create Load
                LoadNumber loadNumber = LoadNumber.of(loadNumberStr);
                Load load = Load.builder().id(LoadId.generate()).tenantId(command.getTenantId()).loadNumber(loadNumber).build();

                // Create Orders for this load
                for (Map.Entry<String, List<PickingListCsvRow>> orderEntry : rowsByOrder.entrySet()) {
                    String orderNumberStr = orderEntry.getKey();
                    List<PickingListCsvRow> orderRows = orderEntry.getValue();

                    // Get first row for order-level data
                    PickingListCsvRow firstRow = orderRows.get(0);

                    // Validate product codes
                    for (PickingListCsvRow row : orderRows) {
                        if (!productServicePort.productExists(row.getProductCode())) {
                            throw new IllegalArgumentException(String.format("Product does not exist: %s", row.getProductCode()));
                        }
                    }

                    // Create Order
                    OrderNumber orderNumber = OrderNumber.of(orderNumberStr);
                    CustomerInfo customerInfo = CustomerInfo.of(firstRow.getCustomerCode(), firstRow.getCustomerName());
                    Priority priority = firstRow.getPriority() != null ? Priority.fromString(firstRow.getPriority()) : Priority.NORMAL;

                    // Build line items first
                    List<OrderLineItem> lineItems = new ArrayList<>();
                    for (PickingListCsvRow row : orderRows) {
                        OrderLineItem lineItem =
                                OrderLineItem.builder().id(OrderLineItemId.generate()).productCode(ProductCode.of(row.getProductCode())).quantity(Quantity.of(row.getQuantity()))
                                        .notes((Notes) null).build();
                        lineItems.add(lineItem);
                    }

                    // Build order with all line items
                    Order order = Order.builder().id(OrderId.generate()).orderNumber(orderNumber).customerInfo(customerInfo).priority(priority).status(OrderStatus.PENDING)
                            .lineItems(lineItems).build();

                    // Add order to load (will be saved via cascade when picking list is saved)
                    load.addOrder(order);
                }

                // Generate picking list reference
                var pickingListReference = referenceGenerator.generate(command.getTenantId());

                // Create PickingList with this load (load will be saved via cascade)
                PickingList pickingList =
                        PickingList.builder().id(PickingListId.generate()).tenantId(command.getTenantId()).pickingListReference(pickingListReference).load(load).notes((Notes) null)
                                .build();

                // Get domain events BEFORE saving
                List<DomainEvent<?>> domainEvents = List.copyOf(pickingList.getDomainEvents());

                // Save picking list (this will cascade save the load and all orders)
                pickingListRepository.save(pickingList);

                // Collect events for publishing and clear domain events
                allEvents.addAll(domainEvents);
                allEvents.addAll(load.getDomainEvents());
                pickingList.clearDomainEvents();
                load.clearDomainEvents();

                createdPickingListIds.add(pickingList.getId());
                successfulRows += loadRows.size();

            } catch (Exception e) {
                log.error("Error processing load {}: {}", loadNumberStr, e.getMessage(), e);
                errorRows += loadRows.size();
                errors.add(CsvUploadResult.CsvValidationError.builder().rowNumber((int) loadRows.get(0).getRowNumber()).fieldName("Load").errorMessage(e.getMessage())
                        .invalidValue(loadNumberStr).build());
            }
        }

        // 5. Publish events after transaction commit
        if (!allEvents.isEmpty()) {
            publishEventsAfterCommit(allEvents);
        }

        // 6. Return result
        return CsvUploadResult.builder().totalRows(totalRows).successfulRows(successfulRows).errorRows(errorRows).createdPickingListIds(createdPickingListIds).errors(errors)
                .build();
    }

    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "Defensive validation is intentional - validates command integrity even if "
            + "constructor validation exists")
    private void validateCommand(UploadPickingListCsvCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getCsvContent() == null || command.getCsvContent().length == 0) {
            throw new IllegalArgumentException("CSV content is required");
        }
    }

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
