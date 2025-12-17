package com.ccbsa.wms.user.dataaccess.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * JPA Entity: UserEntity
 * <p>
 * JPA representation of User aggregate.
 * <p>
 * Note: User is tenant-aware. Uses schema-per-tenant strategy via TenantSchemaResolver for multi-tenant isolation. Each tenant has its own isolated PostgreSQL schema. The schema
 * name is resolved dynamically based on the current tenant
 * context.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "users",
        schema = "tenant_schema")
public class UserEntity {
    @Id
    @Column(name = "user_id",
            length = 50,
            nullable = false)
    private String userId;
    @Column(name = "tenant_id",
            length = 50,
            nullable = false)
    private String tenantId;
    @Column(name = "username",
            length = 100,
            nullable = false)
    private String username;
    @Column(name = "email_address",
            length = 255,
            nullable = false)
    private String emailAddress;
    @Column(name = "first_name",
            length = 100)
    private String firstName;
    @Column(name = "last_name",
            length = 100)
    private String lastName;
    @Column(name = "keycloak_user_id",
            length = 100)
    private String keycloakUserId;
    @Enumerated(EnumType.STRING)
    @Column(name = "status",
            length = 20,
            nullable = false)
    private UserStatus status;
    @Column(name = "created_at",
            nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;
    @Version
    @Column(name = "version",
            nullable = false)
    private Long version;

    // JPA requires no-arg constructor
    public UserEntity() {
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getKeycloakUserId() {
        return keycloakUserId;
    }

    public void setKeycloakUserId(String keycloakUserId) {
        this.keycloakUserId = keycloakUserId;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * User status enumeration.
     */
    public enum UserStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }
}

