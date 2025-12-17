package com.ccbsa.wms.product.application.service.port.messaging;

import com.ccbsa.common.messaging.EventPublisher;
import com.ccbsa.wms.product.domain.core.event.ProductEvent;

/**
 * Event Publisher Port: ProductEventPublisher
 * <p>
 * Defines the contract for publishing Product domain events. Implemented by messaging adapters (Kafka).
 * <p>
 * This port extends the common EventPublisher interface and adds service-specific event publishing methods.
 */
public interface ProductEventPublisher
        extends EventPublisher {
    /**
     * Publishes a Product event.
     *
     * @param event Product event to publish
     */
    void publish(ProductEvent event);
}

