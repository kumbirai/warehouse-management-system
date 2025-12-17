package com.ccbsa.wms.user.dataaccess.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.EmailAddress;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.user.dataaccess.entity.UserEntity;
import com.ccbsa.wms.user.domain.core.entity.User;
import com.ccbsa.wms.user.domain.core.valueobject.FirstName;
import com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId;
import com.ccbsa.wms.user.domain.core.valueobject.LastName;
import com.ccbsa.wms.user.domain.core.valueobject.UserStatus;
import com.ccbsa.wms.user.domain.core.valueobject.Username;

/**
 * Mapper: UserEntityMapper
 * <p>
 * Maps between User domain aggregate and UserEntity JPA entity. Handles conversion between domain value objects and JPA entity fields.
 */
@Component
public class UserEntityMapper {

    /**
     * Converts User domain entity to UserEntity JPA entity.
     * <p>
     * For new entities (version == 0), version is not set to let Hibernate manage it. For existing entities (version > 0), version is set to enable optimistic locking.
     *
     * @param user User domain entity
     * @return UserEntity JPA entity
     * @throws IllegalArgumentException if user is null
     */
    public UserEntity toEntity(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        UserEntity entity = new UserEntity();
        entity.setUserId(user.getId()
                .getValue());
        entity.setTenantId(user.getTenantId()
                .getValue());
        entity.setUsername(user.getUsername()
                .getValue());
        entity.setEmailAddress(user.getEmail()
                .getValue());
        entity.setFirstName(user.getFirstName()
                .map(FirstName::getValue)
                .orElse(null));
        entity.setLastName(user.getLastName()
                .map(LastName::getValue)
                .orElse(null));
        entity.setKeycloakUserId(user.getKeycloakUserId()
                .map(KeycloakUserId::getValue)
                .orElse(null));
        entity.setStatus(mapToEntityStatus(user.getStatus()));
        entity.setCreatedAt(user.getCreatedAt());
        entity.setLastModifiedAt(user.getLastModifiedAt());

        // For new entities, version will be set by Hibernate when persisting
        // For existing entities loaded from DB, version is already set
        // We only set version when mapping from domain if it's > 0 (existing entity)
        int domainVersion = user.getVersion();
        if (domainVersion > 0) {
            entity.setVersion(Long.valueOf(domainVersion));
        }
        // For new entities (version == 0), don't set version - let Hibernate manage it

        return entity;
    }

    /**
     * Maps domain UserStatus to JPA entity UserStatus enum.
     *
     * @param domainStatus Domain status
     * @return JPA entity status
     */
    private UserEntity.UserStatus mapToEntityStatus(UserStatus domainStatus) {
        if (domainStatus == null) {
            throw new IllegalArgumentException("UserStatus cannot be null");
        }
        return UserEntity.UserStatus.valueOf(domainStatus.name());
    }

    /**
     * Converts UserEntity JPA entity to User domain entity.
     *
     * @param entity UserEntity JPA entity
     * @return User domain entity
     * @throws IllegalArgumentException if entity is null
     */
    public User toDomain(UserEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("UserEntity cannot be null");
        }

        return User.builder()
                .userId(UserId.of(entity.getUserId()))
                .tenantId(TenantId.of(entity.getTenantId()))
                .username(Username.of(entity.getUsername()))
                .email(EmailAddress.of(entity.getEmailAddress()))
                .firstName(FirstName.of(entity.getFirstName()))
                .lastName(LastName.of(entity.getLastName()))
                .keycloakUserId(entity.getKeycloakUserId() != null ? KeycloakUserId.of(entity.getKeycloakUserId()) : null)
                .status(mapToDomainStatus(entity.getStatus()))
                .createdAt(entity.getCreatedAt())
                .lastModifiedAt(entity.getLastModifiedAt())
                .version(entity.getVersion() != null ? entity.getVersion()
                        .intValue() : 0)
                .buildWithoutEvents();
    }

    /**
     * Maps JPA entity UserStatus enum to domain UserStatus.
     *
     * @param entityStatus JPA entity status
     * @return Domain status
     */
    private UserStatus mapToDomainStatus(UserEntity.UserStatus entityStatus) {
        if (entityStatus == null) {
            throw new IllegalArgumentException("UserStatus cannot be null");
        }
        return UserStatus.valueOf(entityStatus.name());
    }
}

