package com.ccbsa.wms.notification.email.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration: EmailConfigurationProperties
 * <p>
 * Email configuration properties at notification.email level.
 * Configured via application.yml with prefix: notification.email
 */
@ConfigurationProperties(prefix = "notification.email")
@Validated
public class EmailConfigurationProperties {
    private boolean enabled = true;
    private String replyTo;
    private String supportEmail;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }
}

