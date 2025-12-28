package com.ccbsa.wms.user.dataaccess.entity;

import java.time.LocalDateTime;

import com.ccbsa.wms.user.domain.core.valueobject.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity: UserViewEntity
 * <p>
 * Read model entity for user queries. This is a denormalized view optimized for read operations.
 * <p>
 * Note: Currently maps to the users table. In the future, this can be migrated to a separate
 * user_views table that is maintained via event listeners for better read/write separation.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with
 * the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "users", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class UserViewEntity {
    @Id
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;

    @Column(name = "username", length = 100, nullable = false)
    private String username;

    @Column(name = "email_address", length = 255, nullable = false)
    private String emailAddress;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "keycloak_user_id", length = 100)
    private String keycloakUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private UserStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;
}

