package com.ccbsa.wms.tenant.domain.core.entity;

import java.time.LocalDateTime;
import java.util.Locale;

import com.ccbsa.common.domain.AggregateRoot;
import com.ccbsa.common.domain.valueobject.EmailAddress;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.domain.core.event.TenantActivatedEvent;
import com.ccbsa.wms.tenant.domain.core.event.TenantConfigurationUpdatedEvent;
import com.ccbsa.wms.tenant.domain.core.event.TenantCreatedEvent;
import com.ccbsa.wms.tenant.domain.core.event.TenantDeactivatedEvent;
import com.ccbsa.wms.tenant.domain.core.event.TenantSchemaCreatedEvent;
import com.ccbsa.wms.tenant.domain.core.event.TenantSuspendedEvent;
import com.ccbsa.wms.tenant.domain.core.valueobject.ContactInformation;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantConfiguration;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantName;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantStatus;

/**
 * Aggregate Root: Tenant
 * <p>
 * Represents a tenant (LDP - Local Distribution Partner) with lifecycle and configuration.
 * <p>
 * Business Rules: - Tenant ID must be unique - Tenant name is required - Tenant status transitions must be valid: - PENDING → ACTIVE - ACTIVE → INACTIVE or SUSPENDED - SUSPENDED →
 * ACTIVE or INACTIVE - INACTIVE → ACTIVE - Cannot delete
 * active tenant (must deactivate first) - Tenant schema must be created during activation (for schema-per-tenant)
 * <p>
 * Note: Tenant is NOT tenant-aware (it IS the tenant), so it extends AggregateRoot, not TenantAwareAggregateRoot.
 */
public class Tenant
        extends AggregateRoot<TenantId> {
    // Value Objects
    private TenantName name;
    private TenantStatus status;
    private ContactInformation contactInformation;
    private TenantConfiguration configuration;
    // Primitives
    private LocalDateTime createdAt;
    private LocalDateTime activatedAt;
    private LocalDateTime deactivatedAt;

    /**
     * Private constructor for builder pattern. Prevents direct instantiation.
     */
    private Tenant() {
        // Builder will set all fields
    }

    /**
     * Factory method to create builder instance.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Business logic method: Activates the tenant.
     * <p>
     * Business Rules: - Only PENDING tenants can be activated - Tenant must not already be ACTIVE
     *
     * @throws IllegalStateException if tenant cannot be activated
     */
    public void activate() {
        if (this.status == TenantStatus.ACTIVE) {
            throw new IllegalStateException("Tenant is already active");
        }
        if (!this.status.canTransitionTo(TenantStatus.ACTIVE)) {
            throw new IllegalStateException(String.format("Cannot activate tenant: invalid status transition from %s", this.status));
        }

        this.status = TenantStatus.ACTIVE;
        this.activatedAt = LocalDateTime.now();
        incrementVersion();

        // Publish domain event
        addDomainEvent(new TenantSchemaCreatedEvent(this.getId(), resolveSchemaName()));
        addDomainEvent(new TenantActivatedEvent(this.getId()));
    }

    @Override
    public TenantId getId() {
        return super.getId();
    }

    /**
     * Resolves the schema name for this tenant.
     * <p>
     * Schema naming convention: `tenant_{sanitized_tenant_id}_schema`
     * <p>
     * This matches the convention used by TenantSchemaResolver in common-dataaccess. The tenant ID is sanitized to ensure it's a valid PostgreSQL identifier.
     *
     * @return Schema name in format: `tenant_{sanitized_tenant_id}_schema`
     */
    String resolveSchemaName() {
        String tenantIdValue = this.getId()
                .getValue();

        // Sanitize tenant ID to ensure valid PostgreSQL identifier
        // Convert to lowercase and replace special characters with underscores
        String sanitized = tenantIdValue.toLowerCase(Locale.ROOT)
                .replace("-", "_")
                .replace(".", "_")
                .replace(" ", "_");

        // Remove any remaining invalid characters (keep only alphanumeric and underscores)
        sanitized = sanitized.replaceAll("[^a-z0-9_]", "");

        // Ensure it starts with a letter or underscore (PostgreSQL requirement)
        if (sanitized.isEmpty() || Character.isDigit(sanitized.charAt(0))) {
            sanitized = String.format("t_%s", sanitized);
        }

        // Ensure it doesn't exceed PostgreSQL identifier length limit (63 characters)
        // Account for "tenant_" prefix and "_schema" suffix (14 characters total)
        int maxLength = 63 - 14;
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength);
        }

        // Return schema name with _schema suffix to match TenantSchemaResolver convention
        return String.format("tenant_%s_schema", sanitized);
    }

    /**
     * Business logic method: Deactivates the tenant.
     * <p>
     * Business Rules: - Only ACTIVE or SUSPENDED tenants can be deactivated - Tenant must not already be INACTIVE
     *
     * @throws IllegalStateException if tenant cannot be deactivated
     */
    public void deactivate() {
        if (this.status == TenantStatus.INACTIVE) {
            throw new IllegalStateException("Tenant is already inactive");
        }
        if (!this.status.canTransitionTo(TenantStatus.INACTIVE)) {
            throw new IllegalStateException(String.format("Cannot deactivate tenant: invalid status transition from %s", this.status));
        }

        this.status = TenantStatus.INACTIVE;
        this.deactivatedAt = LocalDateTime.now();
        incrementVersion();

        // Publish domain event
        addDomainEvent(new TenantDeactivatedEvent(this.getId()));
    }

    /**
     * Business logic method: Suspends the tenant.
     * <p>
     * Business Rules: - Only ACTIVE tenants can be suspended - Tenant must not already be SUSPENDED
     *
     * @throws IllegalStateException if tenant cannot be suspended
     */
    public void suspend() {
        if (this.status == TenantStatus.SUSPENDED) {
            throw new IllegalStateException("Tenant is already suspended");
        }
        if (!this.status.canTransitionTo(TenantStatus.SUSPENDED)) {
            throw new IllegalStateException(String.format("Cannot suspend tenant: invalid status transition from %s", this.status));
        }

        this.status = TenantStatus.SUSPENDED;
        incrementVersion();

        // Publish domain event
        addDomainEvent(new TenantSuspendedEvent(this.getId()));
    }

    /**
     * Business logic method: Updates tenant configuration.
     *
     * @param newConfiguration New tenant configuration
     * @throws IllegalArgumentException if configuration is null
     */
    public void updateConfiguration(TenantConfiguration newConfiguration) {
        if (newConfiguration == null) {
            throw new IllegalArgumentException("Tenant configuration cannot be null");
        }

        this.configuration = newConfiguration;
        incrementVersion();

        // Publish domain event
        addDomainEvent(new TenantConfigurationUpdatedEvent(this.getId(), newConfiguration));
    }

    /**
     * Query method: Checks if tenant can be activated.
     *
     * @return true if tenant can be activated
     */
    public boolean canActivate() {
        return this.status == TenantStatus.PENDING;
    }

    /**
     * Query method: Checks if tenant can be deactivated.
     *
     * @return true if tenant can be deactivated
     */
    public boolean canDeactivate() {
        return this.status == TenantStatus.ACTIVE || this.status == TenantStatus.SUSPENDED;
    }

    /**
     * Query method: Checks if tenant can be suspended.
     *
     * @return true if tenant can be suspended
     */
    public boolean canSuspend() {
        return this.status == TenantStatus.ACTIVE;
    }

    /**
     * Query method: Checks if tenant is active.
     *
     * @return true if tenant status is ACTIVE
     */
    public boolean isActive() {
        return this.status == TenantStatus.ACTIVE;
    }

    // Getters (read-only access)

    /**
     * Query method: Checks if tenant is pending.
     *
     * @return true if tenant status is PENDING
     */
    public boolean isPending() {
        return this.status == TenantStatus.PENDING;
    }

    public TenantName getName() {
        return name;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public ContactInformation getContactInformation() {
        return contactInformation;
    }

    public TenantConfiguration getConfiguration() {
        return configuration;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public LocalDateTime getDeactivatedAt() {
        return deactivatedAt;
    }

    /**
     * Builder class for constructing Tenant instances. Ensures all required fields are set and validated.
     */
    public static class Builder {
        private Tenant tenant = new Tenant();

        public Builder tenantId(TenantId tenantId) {
            tenant.setId(tenantId);
            return this;
        }

        public Builder name(TenantName name) {
            tenant.name = name;
            return this;
        }

        public Builder contactInformation(ContactInformation contactInformation) {
            tenant.contactInformation = contactInformation;
            return this;
        }

        public Builder configuration(TenantConfiguration configuration) {
            tenant.configuration = configuration;
            return this;
        }

        public Builder status(TenantStatus status) {
            tenant.status = status;
            return this;
        }

        /**
         * Sets the creation timestamp (for loading from database).
         *
         * @param createdAt Creation timestamp
         * @return Builder instance
         */
        public Builder createdAt(LocalDateTime createdAt) {
            tenant.createdAt = createdAt;
            return this;
        }

        /**
         * Sets the activation timestamp (for loading from database).
         *
         * @param activatedAt Activation timestamp
         * @return Builder instance
         */
        public Builder activatedAt(LocalDateTime activatedAt) {
            tenant.activatedAt = activatedAt;
            return this;
        }

        /**
         * Sets the deactivation timestamp (for loading from database).
         *
         * @param deactivatedAt Deactivation timestamp
         * @return Builder instance
         */
        public Builder deactivatedAt(LocalDateTime deactivatedAt) {
            tenant.deactivatedAt = deactivatedAt;
            return this;
        }

        /**
         * Sets the version (for loading from database).
         *
         * @param version Version number
         * @return Builder instance
         */
        public Builder version(int version) {
            tenant.setVersion(version);
            return this;
        }

        /**
         * Builds and validates the Tenant instance. Publishes creation event for new tenants.
         *
         * @return Validated Tenant instance
         * @throws IllegalArgumentException if validation fails
         */
        public Tenant build() {
            validate();
            initializeDefaults();

            // Set createdAt if not already set (for new tenants)
            if (tenant.createdAt == null) {
                tenant.createdAt = LocalDateTime.now();
            }

            // Publish creation event only if this is a new tenant (no version set)
            if (tenant.getVersion() == 0) {
                // Extract email from contact information if available
                EmailAddress email = null;
                if (tenant.contactInformation != null) {
                    email = tenant.contactInformation.getEmail()
                            .orElse(null);
                }
                tenant.addDomainEvent(new TenantCreatedEvent(tenant.getId(), tenant.name, tenant.status, email));

                // Publish schema creation event for new tenants
                // This triggers schema creation in all tenant-aware services (notification, user, etc.)
                tenant.addDomainEvent(new TenantSchemaCreatedEvent(tenant.getId(), tenant.resolveSchemaName()));
            }

            return consumeTenant();
        }

        /**
         * Validates all required fields are set.
         *
         * @throws IllegalArgumentException if validation fails
         */
        private void validate() {
            if (tenant.getId() == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (tenant.name == null) {
                throw new IllegalArgumentException("Tenant name is required");
            }
        }

        /**
         * Initializes default values for optional fields.
         */
        private void initializeDefaults() {
            if (tenant.status == null) {
                tenant.status = TenantStatus.PENDING;
            }
            if (tenant.configuration == null) {
                tenant.configuration = TenantConfiguration.defaultConfiguration();
            }
        }

        private Tenant consumeTenant() {
            Tenant builtTenant = tenant;
            tenant = new Tenant();
            return builtTenant;
        }

        /**
         * Builds and validates the Tenant instance without publishing events. Used when loading from database.
         *
         * @return Validated Tenant instance
         * @throws IllegalArgumentException if validation fails
         */
        public Tenant buildWithoutEvents() {
            validate();
            initializeDefaults();

            // Set createdAt if not already set
            if (tenant.createdAt == null) {
                tenant.createdAt = LocalDateTime.now();
            }

            // Do not publish events when loading from database
            return consumeTenant();
        }
    }
}

