package com.ccbsa.wms.user.dataaccess.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.user.application.service.port.repository.UserRepository;
import com.ccbsa.wms.user.dataaccess.entity.UserEntity;
import com.ccbsa.wms.user.dataaccess.jpa.UserJpaRepository;
import com.ccbsa.wms.user.dataaccess.mapper.UserEntityMapper;
import com.ccbsa.wms.user.domain.core.entity.User;
import com.ccbsa.wms.user.domain.core.valueobject.FirstName;
import com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId;
import com.ccbsa.wms.user.domain.core.valueobject.LastName;
import com.ccbsa.wms.user.domain.core.valueobject.UserStatus;
import com.ccbsa.wms.user.domain.core.valueobject.Username;

/**
 * Repository Adapter: UserRepositoryAdapter
 * <p>
 * Implements UserRepository port interface.
 * Adapts between domain User aggregate and JPA UserEntity.
 */
@Repository
public class UserRepositoryAdapter implements UserRepository {
    private final UserJpaRepository jpaRepository;
    private final UserEntityMapper mapper;

    public UserRepositoryAdapter(UserJpaRepository jpaRepository,
                                 UserEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(User user) {
        // Check if entity already exists to handle optimistic locking correctly
        Optional<UserEntity> existingEntity = jpaRepository.findById(user.getId().getValue());

        if (existingEntity.isPresent()) {
            // Update existing entity to preserve JPA managed state and version
            UserEntity entity = existingEntity.get();
            updateEntityFromDomain(entity, user);
            jpaRepository.save(entity);
        } else {
            // Create new entity for new users
            UserEntity entity = mapper.toEntity(user);
            jpaRepository.save(entity);
        }
    }

    /**
     * Updates an existing JPA entity from domain object.
     * Preserves JPA managed state and version for optimistic locking.
     *
     * @param entity Existing JPA entity (managed)
     * @param user   Domain user object
     */
    private void updateEntityFromDomain(UserEntity entity, User user) {
        entity.setTenantId(user.getTenantId().getValue());
        entity.setUsername(user.getUsername().getValue());
        entity.setEmailAddress(user.getEmail().getValue());
        entity.setFirstName(user.getFirstName().map(FirstName::getValue).orElse(null));
        entity.setLastName(user.getLastName().map(LastName::getValue).orElse(null));
        entity.setKeycloakUserId(user.getKeycloakUserId().map(KeycloakUserId::getValue).orElse(null));
        entity.setStatus(mapToEntityStatus(user.getStatus()));
        entity.setCreatedAt(user.getCreatedAt());
        entity.setLastModifiedAt(user.getLastModifiedAt());
        // Version is managed by JPA - don't set it manually
        // The version from the domain object should match the entity's current version
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

    @Override
    public Optional<User> findById(UserId userId) {
        return jpaRepository.findById(userId.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByTenantIdAndId(TenantId tenantId, UserId userId) {
        return jpaRepository.findByTenantIdAndUserId(tenantId.getValue(), userId.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public List<User> findByTenantId(TenantId tenantId) {
        return jpaRepository.findByTenantId(tenantId.getValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return jpaRepository.findByUsername(username.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByTenantIdAndUsername(TenantId tenantId, Username username) {
        return jpaRepository.findByTenantIdAndUsername(tenantId.getValue(), username.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findByTenantIdAndStatus(TenantId tenantId, UserStatus status) {
        UserEntity.UserStatus entityStatus = UserEntity.UserStatus.valueOf(status.name());
        return jpaRepository.findByTenantIdAndStatus(tenantId.getValue(), entityStatus)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(UserId userId) {
        return jpaRepository.existsById(userId.getValue());
    }

    @Override
    public boolean existsByTenantIdAndUserId(TenantId tenantId, UserId userId) {
        return jpaRepository.existsByTenantIdAndUserId(tenantId.getValue(), userId.getValue());
    }

    @Override
    public void deleteById(UserId userId) {
        jpaRepository.deleteById(userId.getValue());
    }

    @Override
    public Optional<User> findByKeycloakUserId(String keycloakUserId) {
        return jpaRepository.findByKeycloakUserId(keycloakUserId)
                .map(mapper::toDomain);
    }
}

