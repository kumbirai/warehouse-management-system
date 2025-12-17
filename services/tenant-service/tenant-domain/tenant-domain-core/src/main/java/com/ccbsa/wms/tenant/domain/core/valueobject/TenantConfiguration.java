package com.ccbsa.wms.tenant.domain.core.valueobject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Value Object: TenantConfiguration
 * <p>
 * Represents tenant-specific configuration settings. Immutable and validated on construction.
 */
public final class TenantConfiguration {
    private final String keycloakRealmName;
    private final boolean usePerTenantRealm;
    private final Map<String, String> settings;

    private TenantConfiguration(String keycloakRealmName, boolean usePerTenantRealm, Map<String, String> settings) {
        if (keycloakRealmName != null && keycloakRealmName.trim()
                .isEmpty()) {
            throw new IllegalArgumentException("Keycloak realm name cannot be empty");
        }
        this.keycloakRealmName = keycloakRealmName != null ? keycloakRealmName.trim() : null;
        this.usePerTenantRealm = usePerTenantRealm;
        this.settings = settings != null ? new HashMap<>(settings) : new HashMap<>();
    }

    public static TenantConfiguration defaultConfiguration() {
        return new TenantConfiguration(null, false, new HashMap<>());
    }

    /**
     * Factory method to create a configuration with a per-tenant realm. This explicitly enables per-tenant realm strategy (usePerTenantRealm = true). For single realm strategy
     * (default), use {@link #defaultConfiguration()} or
     * {@link #builder()}.
     *
     * @param keycloakRealmName The realm name for this tenant
     * @return TenantConfiguration with per-tenant realm enabled
     */
    public static TenantConfiguration withRealm(String keycloakRealmName) {
        return new TenantConfiguration(keycloakRealmName, true, new HashMap<>());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> getKeycloakRealmName() {
        return Optional.ofNullable(keycloakRealmName);
    }

    public boolean isUsePerTenantRealm() {
        return usePerTenantRealm;
    }

    public Map<String, String> getSettings() {
        return new HashMap<>(settings); // Defensive copy
    }

    public Optional<String> getSetting(String key) {
        return Optional.ofNullable(settings.get(key));
    }

    public TenantConfiguration withSetting(String key, String value) {
        Map<String, String> newSettings = new HashMap<>(this.settings);
        if (value == null) {
            newSettings.remove(key);
        } else {
            newSettings.put(key, value);
        }
        return new TenantConfiguration(this.keycloakRealmName, this.usePerTenantRealm, newSettings);
    }

    public TenantConfiguration withKeycloakRealmName(String realmName) {
        return new TenantConfiguration(realmName, this.usePerTenantRealm, // Preserve current usePerTenantRealm value
                this.settings);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TenantConfiguration that = (TenantConfiguration) o;
        return usePerTenantRealm == that.usePerTenantRealm && Objects.equals(keycloakRealmName, that.keycloakRealmName) && Objects.equals(settings, that.settings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keycloakRealmName, usePerTenantRealm, settings);
    }

    @Override
    public String toString() {
        return String.format("TenantConfiguration{keycloakRealmName='%s', usePerTenantRealm=%s, settings=%s}", keycloakRealmName, usePerTenantRealm, settings);
    }

    public static class Builder {
        private String keycloakRealmName;
        private boolean usePerTenantRealm = false;
        private Map<String, String> settings = new HashMap<>();

        public Builder keycloakRealmName(String keycloakRealmName) {
            this.keycloakRealmName = keycloakRealmName;
            // Note: Setting realm name does NOT automatically enable per-tenant realms.
            // usePerTenantRealm must be explicitly set to true if per-tenant realms are desired.
            // Default is false (single realm strategy).
            return this;
        }

        public Builder usePerTenantRealm(boolean usePerTenantRealm) {
            this.usePerTenantRealm = usePerTenantRealm;
            return this;
        }

        public Builder setting(String key, String value) {
            this.settings.put(key, value);
            return this;
        }

        public Builder settings(Map<String, String> settings) {
            this.settings = new HashMap<>(settings);
            return this;
        }

        public TenantConfiguration build() {
            return new TenantConfiguration(keycloakRealmName, usePerTenantRealm, settings);
        }
    }
}

