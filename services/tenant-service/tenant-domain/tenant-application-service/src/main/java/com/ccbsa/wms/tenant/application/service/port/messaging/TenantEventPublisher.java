package com.ccbsa.wms.tenant.application.service.port.messaging;

import com.ccbsa.common.messaging.EventPublisher;
import com.ccbsa.wms.tenant.domain.core.event.TenantEvent;

/**
 * Port interface for publishing Tenant domain events.
 * <p>
 * Extends the common EventPublisher interface for type safety. Implemented by the messaging layer (tenant-messaging).
 */
public interface TenantEventPublisher extends EventPublisher {
    /**
     * Publishes a tenant domain event.
     *
     * @param event Tenant domain event to publish
     */
    void publish(TenantEvent<?> event);
}

