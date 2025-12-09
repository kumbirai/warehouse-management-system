package com.ccbsa.wms.tenant.domain.core.entity;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.domain.core.event.TenantSchemaCreatedEvent;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantName;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantTest {

    @Test
    void activatePublishesSchemaEvent() {
        Tenant tenant = Tenant.builder()
                .tenantId(TenantId.of("ldp-001"))
                .name(TenantName.of("LDP Test"))
                .build();

        tenant.clearDomainEvents();

        tenant.activate();

        List<DomainEvent<?>> events = tenant.getDomainEvents();
        assertTrue(events.stream()
                        .anyMatch(event -> event instanceof TenantSchemaCreatedEvent),
                "Activation should publish TenantSchemaCreatedEvent");
    }
}

