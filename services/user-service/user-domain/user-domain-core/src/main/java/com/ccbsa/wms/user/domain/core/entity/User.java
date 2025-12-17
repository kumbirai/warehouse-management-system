package com.ccbsa.wms.user.domain.core.entity;

import java.time.LocalDateTime;
import java.util.Optional;

import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.common.domain.valueobject.Description;
import com.ccbsa.common.domain.valueobject.EmailAddress;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.user.domain.core.event.UserCreatedEvent;
import com.ccbsa.wms.user.domain.core.event.UserDeactivatedEvent;
import com.ccbsa.wms.user.domain.core.event.UserUpdatedEvent;
import com.ccbsa.wms.user.domain.core.valueobject.FirstName;
import com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId;
import com.ccbsa.wms.user.domain.core.valueobject.LastName;
import com.ccbsa.wms.user.domain.core.valueobject.UserStatus;
import com.ccbsa.wms.user.domain.core.valueobject.Username;

/**
 * Aggregate Root: User
 * <p>
 * Represents a user account with profile information and IAM integration.
 * <p>
 * Business Rules: - User ID must be unique within tenant - Username must be unique within tenant - EmailAddress must be valid format - User status transitions must be valid: -
 * ACTIVE → INACTIVE or SUSPENDED - INACTIVE → ACTIVE - SUSPENDED
 * → ACTIVE or INACTIVE - Keycloak user ID is optional but must be unique if provided
 * <p>
 * Note: User is tenant-aware, so it extends TenantAwareAggregateRoot.
 */
public class User
        extends TenantAwareAggregateRoot<UserId> {

    // Value Objects
    private Username username;
    private EmailAddress emailAddress;
    private UserStatus status;
    private KeycloakUserId keycloakUserId;
    private FirstName firstName;
    private LastName lastName;

    // Primitives
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    /**
     * Private constructor for builder pattern. Prevents direct instantiation.
     */
    private User() {
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
     * Business logic method: Deactivates the user.
     * <p>
     * Business Rules: - Only ACTIVE or SUSPENDED users can be deactivated - User must not already be INACTIVE
     *
     * @throws IllegalStateException if user cannot be deactivated
     */
    public void deactivate() {
        if (this.status == UserStatus.INACTIVE) {
            throw new IllegalStateException("User is already inactive");
        }
        if (!this.status.canTransitionTo(UserStatus.INACTIVE)) {
            throw new IllegalStateException(String.format("Cannot deactivate user: invalid status transition from %s", this.status));
        }

        this.status = UserStatus.INACTIVE;
        this.lastModifiedAt = LocalDateTime.now();
        incrementVersion();

        // Publish domain event
        addDomainEvent(new UserDeactivatedEvent(this.getId(), this.getTenantId()));
    }

    @Override
    public UserId getId() {
        return super.getId();
    }

    /**
     * Business logic method: Activates the user.
     * <p>
     * Business Rules: - Only INACTIVE users can be activated - User must not already be ACTIVE
     *
     * @throws IllegalStateException if user cannot be activated
     */
    public void activate() {
        if (this.status == UserStatus.ACTIVE) {
            throw new IllegalStateException("User is already active");
        }
        if (!this.status.canTransitionTo(UserStatus.ACTIVE)) {
            throw new IllegalStateException(String.format("Cannot activate user: invalid status transition from %s", this.status));
        }

        this.status = UserStatus.ACTIVE;
        this.lastModifiedAt = LocalDateTime.now();
        incrementVersion();

        // Publish domain event
        addDomainEvent(new UserUpdatedEvent(this.getId(), this.getTenantId(), this.status, Description.of("User activated")));
    }

    /**
     * Business logic method: Suspends the user.
     * <p>
     * Business Rules: - Only ACTIVE users can be suspended - User must not already be SUSPENDED
     *
     * @throws IllegalStateException if user cannot be suspended
     */
    public void suspend() {
        if (this.status == UserStatus.SUSPENDED) {
            throw new IllegalStateException("User is already suspended");
        }
        if (!this.status.canTransitionTo(UserStatus.SUSPENDED)) {
            throw new IllegalStateException(String.format("Cannot suspend user: invalid status transition from %s", this.status));
        }

        this.status = UserStatus.SUSPENDED;
        this.lastModifiedAt = LocalDateTime.now();
        incrementVersion();

        // Publish domain event
        addDomainEvent(new UserUpdatedEvent(this.getId(), this.getTenantId(), this.status, Description.of("User suspended")));
    }

    /**
     * Business logic method: Updates user profile information.
     *
     * @param emailAddress New emailAddress address
     * @param firstName    New first name (optional, can be null)
     * @param lastName     New last name (optional, can be null)
     * @throws IllegalArgumentException if emailAddress is null
     */
    public void updateProfile(EmailAddress emailAddress, FirstName firstName, LastName lastName) {
        if (emailAddress == null) {
            throw new IllegalArgumentException("EmailAddress cannot be null");
        }

        this.emailAddress = emailAddress;
        this.firstName = firstName;
        this.lastName = lastName;
        this.lastModifiedAt = LocalDateTime.now();
        incrementVersion();

        // Publish domain event
        addDomainEvent(new UserUpdatedEvent(this.getId(), this.getTenantId(), this.status, Description.of("User profile updated")));
    }

    /**
     * Business logic method: Links Keycloak user ID.
     *
     * @param keycloakUserId Keycloak user identifier
     * @throws IllegalArgumentException if keycloakUserId is null
     */
    public void linkKeycloakUser(KeycloakUserId keycloakUserId) {
        if (keycloakUserId == null) {
            throw new IllegalArgumentException("Keycloak user ID cannot be null");
        }

        this.keycloakUserId = keycloakUserId;
        this.lastModifiedAt = LocalDateTime.now();
        incrementVersion();

        // Publish domain event
        addDomainEvent(new UserUpdatedEvent(this.getId(), this.getTenantId(), this.status, Description.of("Keycloak user linked")));
    }

    /**
     * Query method: Checks if user can be activated.
     *
     * @return true if user can be activated
     */
    public boolean canActivate() {
        return this.status == UserStatus.INACTIVE;
    }

    /**
     * Query method: Checks if user can be deactivated.
     *
     * @return true if user can be deactivated
     */
    public boolean canDeactivate() {
        return this.status == UserStatus.ACTIVE || this.status == UserStatus.SUSPENDED;
    }

    /**
     * Query method: Checks if user can be suspended.
     *
     * @return true if user can be suspended
     */
    public boolean canSuspend() {
        return this.status == UserStatus.ACTIVE;
    }

    /**
     * Query method: Checks if user is active.
     *
     * @return true if user status is ACTIVE
     */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    /**
     * Query method: Checks if user is inactive.
     *
     * @return true if user status is INACTIVE
     */
    public boolean isInactive() {
        return this.status == UserStatus.INACTIVE;
    }

    // Getters (read-only access)

    /**
     * Query method: Checks if user is suspended.
     *
     * @return true if user status is SUSPENDED
     */
    public boolean isSuspended() {
        return this.status == UserStatus.SUSPENDED;
    }

    public Username getUsername() {
        return username;
    }

    public EmailAddress getEmail() {
        return emailAddress;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Optional<KeycloakUserId> getKeycloakUserId() {
        return Optional.ofNullable(keycloakUserId);
    }

    public Optional<FirstName> getFirstName() {
        return Optional.ofNullable(firstName);
    }

    public Optional<LastName> getLastName() {
        return Optional.ofNullable(lastName);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    /**
     * Builder class for constructing User instances. Ensures all required fields are set and validated.
     */
    public static class Builder {
        private User user = new User();

        public Builder userId(UserId userId) {
            user.setId(userId);
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            user.setTenantId(tenantId);
            return this;
        }

        public Builder username(Username username) {
            user.username = username;
            return this;
        }

        public Builder email(EmailAddress emailAddress) {
            user.emailAddress = emailAddress;
            return this;
        }

        public Builder status(UserStatus status) {
            user.status = status;
            return this;
        }

        public Builder keycloakUserId(KeycloakUserId keycloakUserId) {
            user.keycloakUserId = keycloakUserId;
            return this;
        }

        public Builder firstName(FirstName firstName) {
            user.firstName = firstName;
            return this;
        }

        public Builder firstName(String firstName) {
            user.firstName = FirstName.of(firstName);
            return this;
        }

        public Builder lastName(LastName lastName) {
            user.lastName = lastName;
            return this;
        }

        public Builder lastName(String lastName) {
            user.lastName = LastName.of(lastName);
            return this;
        }

        /**
         * Sets the creation timestamp (for loading from database).
         *
         * @param createdAt Creation timestamp
         * @return Builder instance
         */
        public Builder createdAt(LocalDateTime createdAt) {
            user.createdAt = createdAt;
            return this;
        }

        /**
         * Sets the last modified timestamp (for loading from database).
         *
         * @param lastModifiedAt Last modified timestamp
         * @return Builder instance
         */
        public Builder lastModifiedAt(LocalDateTime lastModifiedAt) {
            user.lastModifiedAt = lastModifiedAt;
            return this;
        }

        /**
         * Sets the version (for loading from database).
         *
         * @param version Version number
         * @return Builder instance
         */
        public Builder version(int version) {
            user.setVersion(version);
            return this;
        }

        /**
         * Sets the version as Long (for loading from database).
         *
         * @param version Version number
         * @return Builder instance
         */
        public Builder version(Long version) {
            user.setVersion(version != null ? version.intValue() : 0);
            return this;
        }

        /**
         * Builds and validates the User instance. Publishes creation event for new users.
         *
         * @return Validated User instance
         * @throws IllegalArgumentException if validation fails
         */
        public User build() {
            validate();
            initializeDefaults();

            // Set createdAt if not already set (for new users)
            if (user.createdAt == null) {
                user.createdAt = LocalDateTime.now();
            }

            // Publish creation event only if this is a new user (no version set)
            if (user.getVersion() == 0) {
                user.addDomainEvent(new UserCreatedEvent(user.getId(), user.getTenantId(), user.username, user.emailAddress, user.status));
            }

            return consumeUser();
        }

        /**
         * Validates all required fields are set.
         *
         * @throws IllegalArgumentException if validation fails
         */
        private void validate() {
            if (user.getId() == null) {
                throw new IllegalArgumentException("UserId is required");
            }
            if (user.getTenantId() == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (user.username == null) {
                throw new IllegalArgumentException("Username is required");
            }
            if (user.emailAddress == null) {
                throw new IllegalArgumentException("EmailAddress is required");
            }
        }

        /**
         * Initializes default values for optional fields.
         */
        private void initializeDefaults() {
            if (user.status == null) {
                user.status = UserStatus.ACTIVE;
            }
        }

        private User consumeUser() {
            User builtUser = user;
            user = new User();
            return builtUser;
        }

        /**
         * Builds and validates the User instance without publishing events. Used when loading from database.
         *
         * @return Validated User instance
         * @throws IllegalArgumentException if validation fails
         */
        public User buildWithoutEvents() {
            validate();
            initializeDefaults();

            // Set createdAt if not already set
            if (user.createdAt == null) {
                user.createdAt = LocalDateTime.now();
            }

            // Do not publish events when loading from database
            return consumeUser();
        }
    }
}

