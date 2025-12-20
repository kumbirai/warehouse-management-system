package com.ccbsa.wms.stock.application.service.port.messaging;

import com.ccbsa.common.messaging.EventPublisher;
import com.ccbsa.wms.stock.domain.core.event.StockManagementEvent;

/**
 * Event Publisher Port: StockManagementEventPublisher
 * <p>
 * Defines the contract for publishing Stock Management domain events. Extends the base EventPublisher interface from common-messaging.
 * <p>
 * This port is defined in the application service layer and implemented by messaging adapters (Kafka publishers).
 */
public interface StockManagementEventPublisher extends EventPublisher {
    /**
     * Publishes a Stock Management domain event.
     *
     * @param event Stock Management event to publish
     */
    void publish(StockManagementEvent<?> event);
}

