package com.ccbsa.common.keycloak.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Configuration properties for Keycloak Admin Client.
 * <p>
 * These properties are used to configure the connection to Keycloak for administrative operations (user management, realm management, etc.).
 * <p>
 * Note: This class should be enabled via @EnableConfigurationProperties in the application class. Do not annotate with @Configuration to avoid duplicate bean creation.
 */
@ConfigurationProperties(prefix = "keycloak.admin")
@Validated
public class KeycloakConfig {
    @NotBlank(message = "Keycloak server URL is required")
    private String serverUrl;
    @NotBlank(message = "Admin realm is required")
    private String adminRealm = "master";
    @NotBlank(message = "Admin username is required")
    private String adminUsername;
    @NotBlank(message = "Admin password is required")
    private String adminPassword;
    @NotBlank(message = "Admin client ID is required")
    private String adminClientId = "admin-cli";
    private int connectionTimeout = 5000;
    private int socketTimeout = 5000;
    /**
     * Default realm name for user operations when tenant-specific realm is not available. This is used in the single-realm approach or as a fallback.
     */
    private String defaultRealm = "wms-realm";
    /**
     * Client secret for confidential clients (e.g., wms-api). Required for token endpoint authentication when using confidential clients.
     */
    private String clientSecret;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getAdminRealm() {
        return adminRealm;
    }

    public void setAdminRealm(String adminRealm) {
        this.adminRealm = adminRealm;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getAdminClientId() {
        return adminClientId;
    }

    public void setAdminClientId(String adminClientId) {
        this.adminClientId = adminClientId;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public String getDefaultRealm() {
        return defaultRealm;
    }

    public void setDefaultRealm(String defaultRealm) {
        this.defaultRealm = defaultRealm;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}

