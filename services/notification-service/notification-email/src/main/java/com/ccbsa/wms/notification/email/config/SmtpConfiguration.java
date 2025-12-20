package com.ccbsa.wms.notification.email.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration: SmtpConfiguration
 * <p>
 * SMTP configuration properties for email delivery. Configured via application.yml with prefix: notification.email.smtp
 */
@ConfigurationProperties(prefix = "notification.email.smtp")
@Validated
public class SmtpConfiguration {
    @NotBlank(message = "SMTP host cannot be blank")
    private String host = "localhost";

    @NotNull(message = "SMTP port cannot be null")
    @Min(value = 1, message = "SMTP port must be positive")
    private int port = 1025;

    private String username;

    private String password;

    @NotBlank(message = "From address cannot be blank")
    private String fromAddress = "noreply@wms.local";

    private boolean enabled = true;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
