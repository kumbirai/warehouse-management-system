package com.ccbsa.wms.notification.email.template;

/**
 * Exception: TemplateException
 * <p>
 * Thrown when template rendering fails.
 */
public class TemplateException
        extends RuntimeException {
    public TemplateException(String message) {
        super(message);
    }

    public TemplateException(String message, Throwable cause) {
        super(message, cause);
    }
}

