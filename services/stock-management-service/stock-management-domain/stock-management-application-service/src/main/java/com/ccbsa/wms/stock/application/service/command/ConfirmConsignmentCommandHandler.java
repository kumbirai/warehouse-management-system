package com.ccbsa.wms.stock.application.service.command;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.stock.application.service.command.dto.ConfirmConsignmentCommand;
import com.ccbsa.wms.stock.application.service.command.dto.ConfirmConsignmentResult;
import com.ccbsa.wms.stock.application.service.port.messaging.StockManagementEventPublisher;
import com.ccbsa.wms.stock.application.service.port.repository.StockConsignmentRepository;
import com.ccbsa.wms.stock.domain.core.entity.StockConsignment;
import com.ccbsa.wms.stock.domain.core.exception.ConsignmentNotFoundException;

/**
 * Command Handler: ConfirmConsignmentCommandHandler
 * <p>
 * Handles confirmation of stock consignment receipt.
 * <p>
 * Responsibilities:
 * - Validate consignment exists and is in RECEIVED status
 * - Confirm consignment (business logic in aggregate)
 * - Persist consignment
 * - Publish StockConsignmentConfirmedEvent
 * <p>
 * Note: Stock item creation is triggered by event listener (event-driven choreography)
 */
@Component
public class ConfirmConsignmentCommandHandler {
    private final StockConsignmentRepository consignmentRepository;
    private final StockManagementEventPublisher eventPublisher;

    public ConfirmConsignmentCommandHandler(StockConsignmentRepository consignmentRepository, StockManagementEventPublisher eventPublisher) {
        this.consignmentRepository = consignmentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ConfirmConsignmentResult handle(ConfirmConsignmentCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Get consignment
        StockConsignment consignment = consignmentRepository.findByIdAndTenantId(command.getConsignmentId(), command.getTenantId())
                .orElseThrow(() -> new ConsignmentNotFoundException(String.format("Consignment not found: %s", command.getConsignmentId().getValueAsString())));

        // 3. Validate tenant
        if (!consignment.getTenantId().equals(command.getTenantId())) {
            throw new IllegalStateException("Consignment does not belong to tenant");
        }

        // 4. Confirm consignment (business logic in aggregate)
        consignment.confirm();

        // 5. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = List.copyOf(consignment.getDomainEvents());

        // 6. Persist consignment
        consignmentRepository.save(consignment);

        // 7. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            eventPublisher.publish(domainEvents);
            consignment.clearDomainEvents();
        }

        // 8. Return result
        return ConfirmConsignmentResult.builder().consignmentId(consignment.getId()).status(consignment.getStatus()).confirmedAt(consignment.getConfirmedAt()).build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(ConfirmConsignmentCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getConsignmentId() == null) {
            throw new IllegalArgumentException("ConsignmentId is required");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
    }
}

