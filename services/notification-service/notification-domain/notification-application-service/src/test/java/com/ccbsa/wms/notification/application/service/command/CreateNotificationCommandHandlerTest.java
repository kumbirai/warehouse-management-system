package com.ccbsa.wms.notification.application.service.command;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.Message;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.Title;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.notification.application.service.command.dto.CreateNotificationCommand;
import com.ccbsa.wms.notification.application.service.port.messaging.NotificationEventPublisher;
import com.ccbsa.wms.notification.application.service.port.repository.NotificationRepository;
import com.ccbsa.wms.notification.domain.core.entity.Notification;
import com.ccbsa.wms.notification.domain.core.event.NotificationCreatedEvent;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateNotificationCommandHandler")
class CreateNotificationCommandHandlerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationEventPublisher eventPublisher;

    @InjectMocks
    private CreateNotificationCommandHandler handler;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    @DisplayName("publishes NotificationCreatedEvent after commit without losing events")
    @SuppressWarnings("unchecked")
    void shouldPublishNotificationCreatedEventAfterCommit() {
        // Given
        TenantId tenantId = TenantId.of(UUID.randomUUID().toString());
        CreateNotificationCommand command = CreateNotificationCommand.builder()
                .tenantId(tenantId)
                .recipientUserId(UserId.of(UUID.randomUUID().toString()))
                .title(Title.of("Tenant Activated"))
                .message(Message.of("Welcome to WMS"))
                .type(NotificationType.TENANT_ACTIVATED)
                .build();

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        handler.handle(command);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        // Then
        ArgumentCaptor<List<DomainEvent<?>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher).publish(eventsCaptor.capture());

        List<DomainEvent<?>> publishedEvents = eventsCaptor.getValue();
        assertThat(publishedEvents).hasSize(1);
        assertThat(publishedEvents.get(0)).isInstanceOf(NotificationCreatedEvent.class);

        NotificationCreatedEvent event = (NotificationCreatedEvent) publishedEvents.get(0);
        assertThat(event.getTenantId()).isEqualTo(tenantId);
        assertThat(event.getType()).isEqualTo(NotificationType.TENANT_ACTIVATED);
    }
}

