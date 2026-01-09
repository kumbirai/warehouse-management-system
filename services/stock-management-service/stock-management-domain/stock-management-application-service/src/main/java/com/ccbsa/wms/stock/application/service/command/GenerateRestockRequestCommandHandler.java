package com.ccbsa.wms.stock.application.service.command;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.BigDecimalQuantity;
import com.ccbsa.common.domain.valueobject.RestockPriority;
import com.ccbsa.wms.stock.application.service.command.dto.GenerateRestockRequestCommand;
import com.ccbsa.wms.stock.application.service.command.dto.GenerateRestockRequestResult;
import com.ccbsa.wms.stock.application.service.port.messaging.StockManagementEventPublisher;
import com.ccbsa.wms.stock.application.service.port.repository.RestockRequestRepository;
import com.ccbsa.wms.stock.domain.core.entity.RestockRequest;
import com.ccbsa.wms.stock.domain.core.exception.DuplicateRestockRequestException;
import com.ccbsa.wms.stock.domain.core.valueobject.RestockRequestId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: GenerateRestockRequestCommandHandler
 * <p>
 * Handles generation of restock requests when stock falls below minimum.
 * <p>
 * Responsibilities:
 * - Check for existing active restock requests (deduplication)
 * - Calculate priority and requested quantity
 * - Create restock request aggregate
 * - Save restock request
 * - Publish domain events
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GenerateRestockRequestCommandHandler {

    private final RestockRequestRepository restockRequestRepository;
    private final StockManagementEventPublisher eventPublisher;

    @Transactional
    public GenerateRestockRequestResult handle(GenerateRestockRequestCommand command) {
        log.info("Generating restock request for product: {} at location: {}", command.getProductId().getValueAsString(),
                command.getLocationId() != null ? command.getLocationId().getValueAsString() : "all locations");

        // 1. Check for existing active restock request (deduplication)
        Optional<RestockRequest> existingRequest =
                restockRequestRepository.findActiveByProductIdAndLocationId(command.getTenantId(), command.getProductId(), command.getLocationId());

        if (existingRequest.isPresent()) {
            log.info("Active restock request already exists for product: {} at location: {}. Skipping generation.", command.getProductId().getValueAsString(),
                    command.getLocationId() != null ? command.getLocationId().getValueAsString() : "all locations");
            throw new DuplicateRestockRequestException("Active restock request already exists for product: " + command.getProductId().getValueAsString());
        }

        // 2. Calculate priority
        RestockPriority priority = RestockRequest.calculatePriority(command.getCurrentQuantity(), command.getMinimumQuantity());

        // 3. Calculate requested quantity
        BigDecimalQuantity requestedQuantity = RestockRequest.calculateRequestedQuantity(command.getCurrentQuantity(), command.getMinimumQuantity(), command.getMaximumQuantity());

        // 4. Create restock request
        RestockRequest restockRequest = RestockRequest.builder().restockRequestId(RestockRequestId.generate()).tenantId(command.getTenantId()).productId(command.getProductId())
                .locationId(command.getLocationId()).currentQuantity(command.getCurrentQuantity()).minimumQuantity(command.getMinimumQuantity())
                .maximumQuantity(command.getMaximumQuantity()).requestedQuantity(requestedQuantity).priority(priority).build();

        // 5. Save restock request
        restockRequestRepository.save(restockRequest);

        // 6. Publish domain events
        List<DomainEvent<?>> events = restockRequest.getDomainEvents();
        if (!events.isEmpty()) {
            eventPublisher.publish(events);
            restockRequest.clearDomainEvents();
        }

        log.info("Restock request generated successfully: {} with priority: {}", restockRequest.getId().getValueAsString(), priority);

        // 7. Return result
        return GenerateRestockRequestResult.builder().restockRequestId(restockRequest.getId()).priority(priority).isNewRequest(true).build();
    }
}
