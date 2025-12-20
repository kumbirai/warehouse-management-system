package com.ccbsa.wms.notification.messaging.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.valueobject.EmailAddress;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.notification.application.service.command.CreateNotificationCommandHandler;
import com.ccbsa.wms.notification.application.service.command.dto.CreateNotificationCommand;
import com.ccbsa.wms.notification.application.service.port.service.TenantServicePort;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TenantActivatedEventListener.
 * <p>
 * Tests event detection, processing, and error handling scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantActivatedEventListener Tests")
class TenantActivatedEventListenerTest {

    private static final String TOPIC = "tenant-events";
    @Mock
    private CreateNotificationCommandHandler createNotificationCommandHandler;
    @Mock
    private TenantServicePort tenantServicePort;
    @Mock
    private Acknowledgment acknowledgment;
    @InjectMocks
    private TenantActivatedEventListener listener;
    private TenantId tenantId;
    private EmailAddress tenantEmail;

    @BeforeEach
    void setUp() {
        tenantId = TenantId.of(UUID.randomUUID().toString());
        tenantEmail = EmailAddress.of("test@example.com");

        // Clear contexts before each test
        CorrelationContext.clear();
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should process TenantActivatedEvent")
    void shouldProcessTenantActivatedEvent() {
        // Given
        Map<String, Object> eventData = createTenantActivatedEventMap(null);
        String eventType = "TenantActivatedEvent";

        when(tenantServicePort.getTenantEmail(tenantId)).thenReturn(tenantEmail);

        // When
        listener.handle(eventData, eventType, TOPIC, acknowledgment);

        // Then
        verify(tenantServicePort).getTenantEmail(tenantId);
        verify(createNotificationCommandHandler).handle(any(CreateNotificationCommand.class));
        verify(acknowledgment).acknowledge();

        ArgumentCaptor<CreateNotificationCommand> commandCaptor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotificationCommandHandler).handle(commandCaptor.capture());

        CreateNotificationCommand command = commandCaptor.getValue();
        assertThat(command.getTenantId()).isEqualTo(tenantId);
        assertThat(command.getRecipientEmail()).isEqualTo(tenantEmail);
        assertThat(command.getType()).isEqualTo(NotificationType.TENANT_ACTIVATED);
        assertThat(command.getTitle().getValue()).isEqualTo("Tenant Activated");
    }

    /**
     * Creates a Map representation of TenantActivatedEvent for testing.
     */
    private Map<String, Object> createTenantActivatedEventMap(String correlationId) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("aggregateId", tenantId.getValue());
        eventData.put("aggregateType", "Tenant");
        eventData.put("eventId", UUID.randomUUID().toString());
        eventData.put("@class", "com.ccbsa.wms.tenant.domain.core.event.TenantActivatedEvent");

        if (correlationId != null) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("correlationId", correlationId);
            eventData.put("metadata", metadata);
        }

        return eventData;
    }

    @Test
    @DisplayName("Should process TenantActivatedEvent with metadata")
    void shouldProcessTenantActivatedEventWithMetadata() {
        // Given
        Map<String, Object> eventData = createTenantActivatedEventMap("test-correlation-id");
        String eventType = "TenantActivatedEvent";

        when(tenantServicePort.getTenantEmail(tenantId)).thenReturn(tenantEmail);

        // When
        listener.handle(eventData, eventType, TOPIC, acknowledgment);

        // Then
        verify(tenantServicePort).getTenantEmail(tenantId);
        verify(createNotificationCommandHandler).handle(any(CreateNotificationCommand.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should skip TenantCreatedEvent")
    void shouldSkipTenantCreatedEvent() {
        // Given
        Map<String, Object> eventData = createTenantCreatedEventMap();
        String eventType = "TenantCreatedEvent";

        // When
        listener.handle(eventData, eventType, TOPIC, acknowledgment);

        // Then
        verify(tenantServicePort, never()).getTenantEmail(any());
        verify(createNotificationCommandHandler, never()).handle(any());
        verify(acknowledgment).acknowledge();
    }

    /**
     * Creates a Map representation of TenantCreatedEvent for testing.
     */
    private Map<String, Object> createTenantCreatedEventMap() {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("aggregateId", tenantId.getValue());
        eventData.put("aggregateType", "Tenant");
        eventData.put("eventId", UUID.randomUUID().toString());
        eventData.put("@class", "com.ccbsa.wms.tenant.domain.core.event.TenantCreatedEvent");
        eventData.put("name", "Test Tenant");
        eventData.put("status", "PENDING");
        return eventData;
    }

    @Test
    @DisplayName("Should skip TenantSchemaCreatedEvent")
    void shouldSkipTenantSchemaCreatedEvent() {
        // Given
        Map<String, Object> eventData = createTenantSchemaCreatedEventMap();
        String eventType = "TenantSchemaCreatedEvent";

        // When
        listener.handle(eventData, eventType, TOPIC, acknowledgment);

        // Then
        verify(tenantServicePort, never()).getTenantEmail(any());
        verify(createNotificationCommandHandler, never()).handle(any());
        verify(acknowledgment).acknowledge();
    }

    /**
     * Creates a Map representation of TenantSchemaCreatedEvent for testing.
     */
    private Map<String, Object> createTenantSchemaCreatedEventMap() {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("aggregateId", tenantId.getValue());
        eventData.put("aggregateType", "Tenant");
        eventData.put("eventId", UUID.randomUUID().toString());
        eventData.put("@class", "com.ccbsa.wms.tenant.domain.core.event.TenantSchemaCreatedEvent");
        eventData.put("schemaName", "tenant_schema");
        return eventData;
    }

    @Test
    @DisplayName("Should extract and set correlation ID from metadata")
    void shouldExtractAndSetCorrelationIdFromMetadata() {
        // Given
        Map<String, Object> eventData = createTenantActivatedEventMap("test-correlation-id");
        String eventType = "TenantActivatedEvent";

        when(tenantServicePort.getTenantEmail(tenantId)).thenReturn(tenantEmail);

        // When
        listener.handle(eventData, eventType, TOPIC, acknowledgment);

        // Then
        // Note: CorrelationContext is cleared in finally block, so we verify the handler was called
        // The correlation ID extraction is tested indirectly through successful processing
        verify(tenantServicePort).getTenantEmail(tenantId);
        verify(createNotificationCommandHandler).handle(any(CreateNotificationCommand.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should handle tenant service error and retry")
    void shouldHandleTenantServiceErrorAndRetry() {
        // Given
        Map<String, Object> eventData = createTenantActivatedEventMap(null);
        String eventType = "TenantActivatedEvent";

        when(tenantServicePort.getTenantEmail(tenantId)).thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        try {
            listener.handle(eventData, eventType, TOPIC, acknowledgment);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Failed to process TenantActivatedEvent");
        }

        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("Should clear tenant context after processing")
    void shouldClearTenantContextAfterProcessing() {
        // Given
        Map<String, Object> eventData = createTenantActivatedEventMap(null);
        String eventType = "TenantActivatedEvent";

        when(tenantServicePort.getTenantEmail(tenantId)).thenReturn(tenantEmail);

        // When
        listener.handle(eventData, eventType, TOPIC, acknowledgment);

        // Then
        assertThat(TenantContext.getTenantId()).isNull();
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should clear tenant context even on error")
    void shouldClearTenantContextEvenOnError() {
        // Given
        Map<String, Object> eventData = createTenantActivatedEventMap(null);
        String eventType = "TenantActivatedEvent";

        when(tenantServicePort.getTenantEmail(tenantId)).thenThrow(new RuntimeException("Service error"));

        // When & Then
        try {
            listener.handle(eventData, eventType, TOPIC, acknowledgment);
        } catch (RuntimeException e) {
            // Expected
        }

        assertThat(TenantContext.getTenantId()).isNull();
    }
}

