package com.ccbsa.wms.tenant.dataaccess.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.application.service.port.repository.TenantRepository;
import com.ccbsa.wms.tenant.dataaccess.entity.TenantEntity;
import com.ccbsa.wms.tenant.dataaccess.jpa.TenantJpaRepository;
import com.ccbsa.wms.tenant.dataaccess.mapper.TenantEntityMapper;
import com.ccbsa.wms.tenant.domain.core.entity.Tenant;

/**
 * Repository Adapter: TenantRepositoryAdapter
 * <p>
 * Implements TenantRepository port interface. Adapts between domain Tenant aggregate and JPA TenantEntity.
 */
@Repository
public class TenantRepositoryAdapter
        implements TenantRepository {
    private final TenantJpaRepository jpaRepository;
    private final TenantEntityMapper mapper;

    public TenantRepositoryAdapter(TenantJpaRepository jpaRepository, TenantEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(Tenant tenant) {
        // Check if entity already exists to handle updates correctly
        Optional<TenantEntity> existingEntity = jpaRepository.findById(tenant.getId()
                .getValue());

        TenantEntity entity;
        if (existingEntity.isPresent()) {
            // Update existing entity - preserve JPA managed state and version
            entity = existingEntity.get();
            // Update all fields from domain model
            updateEntityFromDomain(entity, tenant);
        } else {
            // New entity - create from domain model
            entity = mapper.toEntity(tenant);
        }

        jpaRepository.save(entity);
    }

    /**
     * Updates an existing entity with values from the domain model. Preserves JPA managed state and version for optimistic locking.
     */
    private void updateEntityFromDomain(TenantEntity entity, Tenant tenant) {
        entity.setName(tenant.getName()
                .getValue());
        entity.setStatus(tenant.getStatus());
        entity.setVersion(tenant.getVersion());

        // Contact information
        if (tenant.getContactInformation() != null) {
            entity.setEmailAddress(tenant.getContactInformation()
                    .getEmailValue()
                    .orElse(null));
            entity.setPhone(tenant.getContactInformation()
                    .getPhone()
                    .orElse(null));
            entity.setAddress(tenant.getContactInformation()
                    .getAddress()
                    .orElse(null));
        } else {
            entity.setEmailAddress(null);
            entity.setPhone(null);
            entity.setAddress(null);
        }

        // Configuration
        if (tenant.getConfiguration() != null) {
            entity.setKeycloakRealmName(tenant.getConfiguration()
                    .getKeycloakRealmName()
                    .orElse(null));
            entity.setUsePerTenantRealm(tenant.getConfiguration()
                    .isUsePerTenantRealm());
        }

        // Timestamps
        entity.setCreatedAt(tenant.getCreatedAt());
        entity.setActivatedAt(tenant.getActivatedAt());
        entity.setDeactivatedAt(tenant.getDeactivatedAt());
    }

    @Override
    public Optional<Tenant> findById(TenantId tenantId) {
        String tenantIdValue = tenantId.getValue();
        return jpaRepository.findById(tenantIdValue)
                .map(mapper::toDomain);
    }

    @Override
    public List<Tenant> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(TenantId tenantId) {
        String tenantIdValue = tenantId.getValue();
        return jpaRepository.existsById(tenantIdValue);
    }

    @Override
    public void deleteById(TenantId tenantId) {
        String tenantIdValue = tenantId.getValue();
        jpaRepository.deleteById(tenantIdValue);
    }
}

