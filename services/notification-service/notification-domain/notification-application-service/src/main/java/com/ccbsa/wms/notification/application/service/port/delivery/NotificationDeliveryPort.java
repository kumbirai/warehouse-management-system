package com.ccbsa.wms.notification.application.service.port.delivery;

import com.ccbsa.wms.notification.application.service.command.dto.DeliveryResult;
import com.ccbsa.wms.notification.domain.core.entity.Notification;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationChannel;

/**
 * Port: NotificationDeliveryPort
 * <p>
 * Defines the contract for notification delivery across different channels (email, SMS, WhatsApp).
 * Implemented by delivery adapters in infrastructure layers.
 * <p>
 * Each adapter implements this port for a specific delivery channel.
 * The application service discovers adapters by calling {@link #supports(NotificationChannel)}.
 */
public interface NotificationDeliveryPort {

    /**
     * Sends a notification via the specified channel.
     *
     * @param notification Notification to send
     * @param channel      Delivery channel (EMAIL, SMS, WHATSAPP)
     * @return Delivery result with status and metadata
     * @throws IllegalArgumentException if notification or channel is null
     */
    DeliveryResult send(Notification notification, NotificationChannel channel);

    /**
     * Checks if this adapter supports the given channel.
     *
     * @param channel Delivery channel to check
     * @return true if this adapter supports the channel, false otherwise
     */
    boolean supports(NotificationChannel channel);
}

