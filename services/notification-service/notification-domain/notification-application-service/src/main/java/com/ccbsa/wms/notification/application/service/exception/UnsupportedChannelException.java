package com.ccbsa.wms.notification.application.service.exception;

import com.ccbsa.wms.notification.domain.core.valueobject.NotificationChannel;

/**
 * Exception: UnsupportedChannelException
 * <p>
 * Thrown when no delivery adapter is available for the requested notification channel.
 */
public class UnsupportedChannelException
        extends RuntimeException {

    public UnsupportedChannelException(NotificationChannel channel) {
        super(String.format("No delivery adapter available for channel: %s", channel));
    }

    public UnsupportedChannelException(NotificationChannel channel, String reason) {
        super(String.format("No delivery adapter available for channel: %s. Reason: %s", channel, reason));
    }
}

