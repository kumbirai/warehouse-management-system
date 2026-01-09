package com.ccbsa.wms.stock.application.service.command;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.stock.application.service.command.dto.CheckExpirationDatesCommand;
import com.ccbsa.wms.stock.application.service.port.messaging.StockManagementEventPublisher;
import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: CheckExpirationDatesCommandHandler
 * <p>
 * Handles batch expiration checking for all stock items.
 * <p>
 * Responsibilities:
 * - Load all stock items from repository
 * - Check expiration dates and reclassify
 * - Save updated stock items
 * - Publish domain events
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CheckExpirationDatesCommandHandler {

    private final StockItemRepository stockItemRepository;
    private final StockManagementEventPublisher eventPublisher;

    @Transactional
    public void handle(CheckExpirationDatesCommand command) {
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.warn("No tenant context available for expiration check");
            return;
        }

        log.info("Starting batch expiration check for tenant: {}", tenantId.getValue());

        // 1. Load all stock items for the current tenant
        List<StockItem> stockItems = stockItemRepository.findByTenantId(tenantId);

        log.info("Found {} stock items to check for tenant: {}", stockItems.size(), tenantId.getValue());

        int updatedCount = 0;
        int eventCount = 0;

        // 2. Check expiration and reclassify each stock item
        for (StockItem stockItem : stockItems) {
            StockClassification oldClassification = stockItem.getClassification();

            // Reclassify - this will check expiration and update classification
            stockItem.classify();

            // 3. Save if classification changed
            if (stockItem.getClassification() != oldClassification) {
                stockItemRepository.save(stockItem);
                updatedCount++;

                // 4. Publish domain events
                List<DomainEvent<?>> events = stockItem.getDomainEvents();
                if (!events.isEmpty()) {
                    eventPublisher.publish(events);
                    eventCount += events.size();
                    stockItem.clearDomainEvents();
                }
            }
        }

        log.info("Completed batch expiration check for tenant: {}. Updated {} stock items, published {} events", tenantId.getValue(), updatedCount, eventCount);
    }
}
