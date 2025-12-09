# Keycloak Integration DRY Strategy

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-01  
**Status:** Approved  
**Related Documents:**

- [Service Architecture Document](Service_Architecture_Document.md)
- [IAM Integration Guide](../03-security/IAM_Integration_Guide.md)
- [Tenant Service Implementation Plan](Tenant_Service_Implementation_Plan.md)

---

## Table of Contents

1. [Problem Statement](#problem-statement)
2. [Keycloak Integration Requirements](#keycloak-integration-requirements)
3. [Solution: Common Keycloak Module](#solution-common-keycloak-module)
4. [Module Structure](#module-structure)
5. [Port/Adapter Pattern](#portadapter-pattern)
6. [Service-Specific Implementations](#service-specific-implementations)
7. [Migration Plan](#migration-plan)

---

## Problem Statement

### Current State

Both `user-service` and `tenant-service` require Keycloak integration:

**User Service:**

- User CRUD operations (create, update, delete users)
- User profile management
- User-tenant relationship management
- Role assignment to users
- User attribute management (including tenant_id)

**Tenant Service:**

- Tenant realm/group creation in Keycloak
- Tenant realm/group activation/deactivation
- Tenant realm/group configuration
- User assignment to tenant groups

### DRY Violation Risk

Without a common module, both services would duplicate:

- Keycloak Admin Client configuration
- Keycloak client setup and connection management
- Common Keycloak utilities (error handling, retry logic)
- Base Keycloak adapter interfaces
- Keycloak configuration properties

---

## Keycloak Integration Requirements

### Common Functionality (Shared)

1. **Keycloak Client Configuration:**
    - Admin client setup
    - Connection configuration (server URL, realm, credentials)
    - Client connection pooling
    - Retry logic and error handling

2. **Base Keycloak Operations:**
    - Get Keycloak realm
    - Check realm/group existence
    - Common error handling
    - Connection health checks

3. **Configuration Properties:**
    - Keycloak server URL
    - Admin realm and credentials
    - Connection timeout settings
    - Retry configuration

### Service-Specific Functionality

**User Service:**

- User CRUD operations
- User attribute management
- Role assignment to users
- User search and filtering

**Tenant Service:**

- Realm/group CRUD operations
- Realm/group activation/deactivation
- Group membership management
- Realm configuration management

---

## Solution: Common Keycloak Module

### Approach

Create a **`common-keycloak`** module that provides:

- Base Keycloak client configuration
- Port interfaces for Keycloak operations
- Common Keycloak utilities
- Service-specific adapters implement ports

### Architecture Pattern

**Port/Adapter Pattern:**

- **Ports** (interfaces) defined in `common-keycloak`
- **Adapters** (implementations) in service-specific modules
- Services depend on ports, not concrete implementations

---

## Module Structure

### Common Keycloak Module (`common/common-keycloak`)

```
common-keycloak/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/
                └── ccbsa/
                    └── common/
                        └── keycloak/
                            ├── config/
                            │   ├── KeycloakConfig.java          # Configuration properties
                            │   └── KeycloakClientConfig.java    # Admin client setup
                            ├── port/
                            │   ├── KeycloakClientPort.java      # Base port interface
                            │   ├── KeycloakRealmPort.java       # Realm operations port
                            │   └── KeycloakGroupPort.java       # Group operations port
                            ├── adapter/
                            │   └── KeycloakClientAdapter.java   # Base adapter implementation
                            └── util/
                                ├── KeycloakExceptionHandler.java # Error handling
                                └── KeycloakRetryUtil.java        # Retry logic
```

### Module Dependencies

**`common-keycloak/pom.xml`:**

```xml
<dependencies>
    <!-- Keycloak Admin Client -->
    <dependency>
        <groupId>org.keycloak</groupId>
        <artifactId>keycloak-admin-client</artifactId>
        <version>${keycloak.version}</version>
    </dependency>
    
    <!-- Spring Boot Configuration -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- Common Domain (for TenantId, UserId) -->
    <dependency>
        <groupId>com.ccbsa.wms</groupId>
        <artifactId>common-domain</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
```

---

## Port/Adapter Pattern

### Base Port Interface

**`KeycloakClientPort.java`:**

```java
package com.ccbsa.common.keycloak.port;

import org.keycloak.admin.client.Keycloak;

/**
 * Port interface for Keycloak client operations.
 * Provides base Keycloak client access.
 */
public interface KeycloakClientPort {
    
    /**
     * Gets the Keycloak admin client instance.
     * 
     * @return Keycloak admin client
     */
    Keycloak getAdminClient();
    
    /**
     * Checks if Keycloak is accessible.
     * 
     * @return true if Keycloak is accessible
     */
    boolean isAccessible();
    
    /**
     * Closes the Keycloak client connection.
     */
    void close();
}
```

### Realm Operations Port

**`KeycloakRealmPort.java`:**

```java
package com.ccbsa.common.keycloak.port;

import com.ccbsa.common.domain.valueobject.TenantId;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * Port interface for Keycloak realm operations.
 * Used by tenant-service for realm management.
 */
public interface KeycloakRealmPort {
    
    /**
     * Creates a realm for a tenant.
     * 
     * @param tenantId Tenant identifier
     * @param realmName Realm name
     * @return Created realm representation
     */
    RealmRepresentation createRealm(TenantId tenantId, String realmName);
    
    /**
     * Gets a realm by name.
     * 
     * @param realmName Realm name
     * @return Realm representation or null if not found
     */
    RealmRepresentation getRealm(String realmName);
    
    /**
     * Updates realm configuration.
     * 
     * @param realmName Realm name
     * @param realm Realm representation with updates
     */
    void updateRealm(String realmName, RealmRepresentation realm);
    
    /**
     * Enables a realm.
     * 
     * @param realmName Realm name
     */
    void enableRealm(String realmName);
    
    /**
     * Disables a realm.
     * 
     * @param realmName Realm name
     */
    void disableRealm(String realmName);
    
    /**
     * Checks if a realm exists.
     * 
     * @param realmName Realm name
     * @return true if realm exists
     */
    boolean realmExists(String realmName);
}
```

### Group Operations Port

**`KeycloakGroupPort.java`:**

```java
package com.ccbsa.common.keycloak.port;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import org.keycloak.representations.idm.GroupRepresentation;

/**
 * Port interface for Keycloak group operations.
 * Used by tenant-service for tenant group management.
 */
public interface KeycloakGroupPort {
    
    /**
     * Creates a group for a tenant.
     * 
     * @param tenantId Tenant identifier
     * @param groupName Group name
     * @return Created group representation
     */
    GroupRepresentation createGroup(TenantId tenantId, String groupName);
    
    /**
     * Gets a group by name.
     * 
     * @param realmName Realm name
     * @param groupName Group name
     * @return Group representation or null if not found
     */
    GroupRepresentation getGroup(String realmName, String groupName);
    
    /**
     * Adds a user to a group.
     * 
     * @param realmName Realm name
     * @param groupId Group ID
     * @param userId User ID
     */
    void addUserToGroup(String realmName, String groupId, UserId userId);
    
    /**
     * Removes a user from a group.
     * 
     * @param realmName Realm name
     * @param groupId Group ID
     * @param userId User ID
     */
    void removeUserFromGroup(String realmName, String groupId, UserId userId);
    
    /**
     * Checks if a group exists.
     * 
     * @param realmName Realm name
     * @param groupName Group name
     * @return true if group exists
     */
    boolean groupExists(String realmName, String groupName);
}
```

### Tenant Service Port

**`TenantServicePort.java`:**

```java
package com.ccbsa.common.keycloak.port;

import com.ccbsa.common.domain.valueobject.TenantId;
import java.util.Optional;

/**
 * Port interface for querying Tenant Service to determine Keycloak realm information.
 * Used by user-service to determine which realm to use for user creation.
 */
public interface TenantServicePort {
    
    /**
     * Gets the Keycloak realm name for a tenant.
     * 
     * @param tenantId Tenant identifier
     * @return Realm name if tenant has a specific realm configured,
     *         or empty if tenant uses the default realm
     */
    Optional<String> getTenantRealmName(TenantId tenantId);
    
    /**
     * Checks if a tenant exists and is ACTIVE.
     * 
     * @param tenantId Tenant identifier
     * @return true if tenant exists and is ACTIVE
     */
    boolean isTenantActive(TenantId tenantId);
    
    /**
     * Gets tenant information including status.
     * 
     * @param tenantId Tenant identifier
     * @return Tenant information or empty if tenant does not exist
     */
    Optional<TenantInfo> getTenantInfo(TenantId tenantId);
}
```

### User Operations Port

**`KeycloakUserPort.java`:**

```java
package com.ccbsa.common.keycloak.port;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.Optional;

/**
 * Port interface for Keycloak user operations.
 * Used by user-service for user management.
 */
public interface KeycloakUserPort {
    
    /**
     * Creates a user in Keycloak.
     * 
     * @param realmName Realm name
     * @param user User representation
     * @return Created user ID
     */
    UserId createUser(String realmName, UserRepresentation user);
    
    /**
     * Gets a user by ID.
     * 
     * @param realmName Realm name
     * @param userId User ID
     * @return User representation or empty if not found
     */
    Optional<UserRepresentation> getUser(String realmName, UserId userId);
    
    /**
     * Gets a user by username.
     * 
     * @param realmName Realm name
     * @param username Username
     * @return User representation or empty if not found
     */
    Optional<UserRepresentation> getUserByUsername(String realmName, String username);
    
    /**
     * Updates a user.
     * 
     * @param realmName Realm name
     * @param userId User ID
     * @param user User representation with updates
     */
    void updateUser(String realmName, UserId userId, UserRepresentation user);
    
    /**
     * Deletes a user.
     * 
     * @param realmName Realm name
     * @param userId User ID
     */
    void deleteUser(String realmName, UserId userId);
    
    /**
     * Sets user password.
     * 
     * @param realmName Realm name
     * @param userId User ID
     * @param password Password
     * @param temporary Whether password is temporary
     */
    void setPassword(String realmName, UserId userId, String password, boolean temporary);
    
    /**
     * Assigns a role to a user.
     * 
     * @param realmName Realm name
     * @param userId User ID
     * @param roleName Role name
     */
    void assignRole(String realmName, UserId userId, String roleName);
    
    /**
     * Removes a role from a user.
     * 
     * @param realmName Realm name
     * @param userId User ID
     * @param roleName Role name
     */
    void removeRole(String realmName, UserId userId, String roleName);
    
    /**
     * Sets user attribute (e.g., tenant_id).
     * 
     * @param realmName Realm name
     * @param userId User ID
     * @param attributeName Attribute name
     * @param attributeValue Attribute value
     */
    void setUserAttribute(String realmName, UserId userId, String attributeName, String attributeValue);
    
    /**
     * Finds users by tenant ID.
     * 
     * @param realmName Realm name
     * @param tenantId Tenant ID
     * @return List of user representations
     */
    List<UserRepresentation> findUsersByTenantId(String realmName, TenantId tenantId);
}
```

---

## Service-Specific Implementations

### User Service Implementation

**Location:** `user-service/user-messaging` (or `user-integration`)

**Adapter:** `KeycloakUserAdapter.java`

```java
package com.ccbsa.wms.user.integration;

import com.ccbsa.common.keycloak.port.KeycloakUserPort;
import com.ccbsa.common.keycloak.port.KeycloakClientPort;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;

@Component
public class KeycloakUserAdapter implements KeycloakUserPort {
    
    private final KeycloakClientPort keycloakClient;
    
    public KeycloakUserAdapter(KeycloakClientPort keycloakClient) {
        this.keycloakClient = keycloakClient;
    }
    
    @Override
    public UserId createUser(String realmName, UserRepresentation user) {
        UsersResource usersResource = keycloakClient.getAdminClient()
            .realm(realmName)
            .users();
        
        var response = usersResource.create(user);
        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
        return UserId.of(userId);
    }
    
    // ... other implementations
}
```

### Tenant Service Implementation

**Location:** `tenant-service/tenant-messaging` (or `tenant-integration`)

**Adapter:** `KeycloakRealmAdapter.java`

```java
package com.ccbsa.wms.tenant.integration;

import com.ccbsa.common.keycloak.port.KeycloakRealmPort;
import com.ccbsa.common.keycloak.port.KeycloakClientPort;
import com.ccbsa.common.domain.valueobject.TenantId;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.stereotype.Component;

@Component
public class KeycloakRealmAdapter implements KeycloakRealmPort {
    
    private final KeycloakClientPort keycloakClient;
    
    public KeycloakRealmAdapter(KeycloakClientPort keycloakClient) {
        this.keycloakClient = keycloakClient;
    }
    
    @Override
    public RealmRepresentation createRealm(TenantId tenantId, String realmName) {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(realmName);
        realm.setEnabled(true);
        
        keycloakClient.getAdminClient().realms().create(realm);
        return getRealm(realmName);
    }
    
    // ... other implementations
}
```

**Adapter:** `KeycloakGroupAdapter.java`

```java
package com.ccbsa.wms.tenant.integration;

import com.ccbsa.common.keycloak.port.KeycloakGroupPort;
import com.ccbsa.common.keycloak.port.KeycloakClientPort;
import org.springframework.stereotype.Component;

@Component
public class KeycloakGroupAdapter implements KeycloakGroupPort {
    
    private final KeycloakClientPort keycloakClient;
    
    public KeycloakGroupAdapter(KeycloakClientPort keycloakClient) {
        this.keycloakClient = keycloakClient;
    }
    
    // ... implementations
}
```

---

## Base Adapter Implementation

### Keycloak Client Adapter

**Location:** `common-keycloak/adapter/KeycloakClientAdapter.java`

```java
package com.ccbsa.common.keycloak.adapter;

import com.ccbsa.common.keycloak.config.KeycloakConfig;
import com.ccbsa.common.keycloak.port.KeycloakClientPort;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.stereotype.Component;

@Component
public class KeycloakClientAdapter implements KeycloakClientPort {
    
    private final KeycloakConfig config;
    private Keycloak keycloak;
    
    public KeycloakClientAdapter(KeycloakConfig config) {
        this.config = config;
        this.keycloak = createKeycloakClient();
    }
    
    @Override
    public Keycloak getAdminClient() {
        if (keycloak == null || isClosed()) {
            keycloak = createKeycloakClient();
        }
        return keycloak;
    }
    
    @Override
    public boolean isAccessible() {
        try {
            keycloak.realms().findAll();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public void close() {
        if (keycloak != null) {
            keycloak.close();
            keycloak = null;
        }
    }
    
    private Keycloak createKeycloakClient() {
        return KeycloakBuilder.builder()
            .serverUrl(config.getServerUrl())
            .realm(config.getAdminRealm())
            .username(config.getAdminUsername())
            .password(config.getAdminPassword())
            .clientId(config.getAdminClientId())
            .build();
    }
    
    private boolean isClosed() {
        // Check if client is closed
        return false; // Simplified
    }
}
```

### Configuration

**Location:** `common-keycloak/config/KeycloakConfig.java`

```java
package com.ccbsa.common.keycloak.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "keycloak.admin")
public class KeycloakConfig {
    
    private String serverUrl;
    private String adminRealm;
    private String adminUsername;
    private String adminPassword;
    private String adminClientId;
    private int connectionTimeout = 5000;
    private int socketTimeout = 5000;
    
    // Getters and setters
    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }
    
    // ... other getters/setters
}
```

**Configuration Properties (`application.yml`):**

```yaml
keycloak:
  admin:
    server-url: ${KEYCLOAK_SERVER_URL:http://localhost:7080}
    admin-realm: ${KEYCLOAK_ADMIN_REALM:master}
    admin-username: ${KEYCLOAK_ADMIN_USERNAME:admin}
    admin-password: ${KEYCLOAK_ADMIN_PASSWORD:admin}
    admin-client-id: ${KEYCLOAK_ADMIN_CLIENT_ID:admin-cli}
    connection-timeout: 5000
    socket-timeout: 5000
```

---

## Service Dependencies

### User Service

**`user-service/user-container/pom.xml`:**

```xml
<dependencies>
    <!-- Common Keycloak -->
    <dependency>
        <groupId>com.ccbsa.wms</groupId>
        <artifactId>common-keycloak</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <!-- User Service Keycloak Adapter (implements KeycloakUserPort) -->
    <!-- Defined in user-messaging or user-integration module -->
</dependencies>
```

### Tenant Service

**`tenant-service/tenant-container/pom.xml`:**

```xml
<dependencies>
    <!-- Common Keycloak -->
    <dependency>
        <groupId>com.ccbsa.wms</groupId>
        <artifactId>common-keycloak</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <!-- Tenant Service Keycloak Adapters (implement KeycloakRealmPort, KeycloakGroupPort) -->
    <!-- Defined in tenant-messaging or tenant-integration module -->
</dependencies>
```

---

## Usage in Application Services

### User Service - Realm Determination Strategy

**Recommended Approach:** User Service queries Tenant Service to determine the correct Keycloak realm.

**`CreateUserCommandHandler.java`:**

```java
@Component
public class CreateUserCommandHandler {
    
    private final KeycloakUserPort keycloakUserPort;
    private final TenantServicePort tenantServicePort;
    private final KeycloakConfig keycloakConfig;
    
    public CreateUserCommandHandler(
            KeycloakUserPort keycloakUserPort,
            TenantServicePort tenantServicePort,
            KeycloakConfig keycloakConfig) {
        this.keycloakUserPort = keycloakUserPort;
        this.tenantServicePort = tenantServicePort;
        this.keycloakConfig = keycloakConfig;
    }
    
    public UserId handle(CreateUserCommand command) {
        // 1. Validate tenant exists and is ACTIVE
        if (!tenantServicePort.isTenantActive(command.getTenantId())) {
            throw new TenantNotActiveException(
                "Tenant " + command.getTenantId() + " is not active");
        }
        
        // 2. Determine realm name from tenant service or use default
        String realmName = tenantServicePort.getTenantRealmName(command.getTenantId())
            .orElseGet(() -> keycloakConfig.getDefaultRealm());
        
        // 3. Create user in Keycloak
        UserRepresentation user = new UserRepresentation();
        user.setUsername(command.getUsername());
        user.setEmail(command.getEmail());
        user.setEnabled(true);
        
        // 4. Set tenant_id attribute (critical for multi-tenancy enforcement)
        user.singleAttribute("tenant_id", command.getTenantId().getValue());
        
        // 5. Create user in determined realm
        UserId userId = keycloakUserPort.createUser(realmName, user);
        
        // 6. Create user in local database
        // ...
        
        return userId;
    }
}
```

**Key Points:**

- User Service queries Tenant Service to get realm name
- Falls back to default realm if tenant doesn't specify one
- Always validates tenant is ACTIVE before user creation
- Always sets `tenant_id` attribute for multi-tenancy enforcement

### Tenant Service - Realm Management

**`ActivateTenantCommandHandler.java`:**

```java
@Component
public class ActivateTenantCommandHandler {
    
    private final KeycloakRealmPort keycloakRealmPort;
    private final TenantRepository tenantRepository;
    
    public ActivateTenantCommandHandler(
            KeycloakRealmPort keycloakRealmPort,
            TenantRepository tenantRepository) {
        this.keycloakRealmPort = keycloakRealmPort;
        this.tenantRepository = tenantRepository;
    }
    
    public void handle(ActivateTenantCommand command) {
        Tenant tenant = tenantRepository.findById(command.getTenantId())
            .orElseThrow(() -> new TenantNotFoundException(command.getTenantId()));
        
        // Activate tenant
        tenant.activate();
        
        // Determine realm strategy from tenant configuration
        TenantConfiguration config = tenant.getConfiguration();
        String realmName = config.getKeycloakRealmName()
            .orElseGet(() -> generateRealmName(tenant.getId()));
        
        // Create or enable Keycloak realm (if using per-tenant realms)
        if (config.isUsePerTenantRealm()) {
            if (!keycloakRealmPort.realmExists(realmName)) {
                keycloakRealmPort.createRealm(tenant.getId(), realmName);
            } else {
                keycloakRealmPort.enableRealm(realmName);
            }
            
            // Store realm name in tenant configuration
            config.setKeycloakRealmName(realmName);
        }
        // If using single realm, no realm creation needed
        
        tenantRepository.save(tenant);
    }
    
    private String generateRealmName(TenantId tenantId) {
        return "tenant-" + tenantId.getValue();
    }
}
```

**Realm Strategy:**

- **Single Realm (Recommended for MVP):** All tenants share `wms-realm`, differentiated by `tenant_id` attribute
- **Per-Tenant Realms:** Each tenant has its own realm (e.g., `tenant-{tenantId}`)
- Tenant configuration determines which strategy to use

---

## Migration Plan

### Phase 1: Create Common Module

1. Create `common-keycloak` module structure
2. Add Keycloak Admin Client dependency
3. Create configuration classes
4. Create port interfaces
5. Create base adapter implementation

### Phase 2: Migrate User Service

1. Add `common-keycloak` dependency to user-service
2. Create `KeycloakUserAdapter` implementing `KeycloakUserPort`
3. Update user service to use `KeycloakUserPort` instead of direct Keycloak client
4. Remove duplicate Keycloak configuration from user-service

### Phase 3: Implement Tenant Service

1. Add `common-keycloak` dependency to tenant-service
2. Create `KeycloakRealmAdapter` implementing `KeycloakRealmPort`
3. Create `KeycloakGroupAdapter` implementing `KeycloakGroupPort`
4. Use ports in tenant service application services

### Phase 4: Cleanup

1. Remove duplicate Keycloak configuration from all services
2. Ensure all services use common configuration
3. Update documentation

---

## Benefits

### DRY Compliance

- **Single Source of Truth:** Keycloak client configuration in one place
- **Reusable Ports:** Port interfaces shared across services
- **Common Utilities:** Error handling and retry logic shared

### Maintainability

- **Centralized Updates:** Keycloak version updates in one module
- **Consistent Behavior:** All services use same Keycloak client setup
- **Easier Testing:** Mock ports for unit testing

### Flexibility

- **Service-Specific Adapters:** Each service implements only needed ports
- **Independent Evolution:** Services can extend ports as needed
- **Clear Boundaries:** Port interfaces define clear contracts

---

## References

- [Service Architecture Document](Service_Architecture_Document.md)
- [IAM Integration Guide](../03-security/IAM_Integration_Guide.md)
- [Tenant Service Implementation Plan](Tenant_Service_Implementation_Plan.md)
- [Port/Adapter Pattern Documentation](Service_Architecture_Document.md#portadapter-architecture)

---

**Document Status:** Approved  
**Last Updated:** 2025-01  
**Next Review:** 2025-04

