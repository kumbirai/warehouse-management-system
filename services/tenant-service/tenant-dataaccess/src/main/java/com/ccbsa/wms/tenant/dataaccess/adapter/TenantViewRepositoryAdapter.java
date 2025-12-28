package com.ccbsa.wms.tenant.dataaccess.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.application.service.port.data.TenantViewRepository;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantListQuery;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantListResult;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantView;
import com.ccbsa.wms.tenant.dataaccess.entity.TenantEntity;
import com.ccbsa.wms.tenant.dataaccess.jpa.TenantJpaRepository;
import com.ccbsa.wms.tenant.dataaccess.mapper.TenantViewMapper;

import lombok.RequiredArgsConstructor;

/**
 * Repository Adapter: TenantViewRepositoryAdapter
 * <p>
 * Implements TenantViewRepository data port interface. Provides read model access to tenant data.
 * <p>
 * Note: Tenant Service is NOT tenant-aware (it manages tenants), so all queries operate on the public schema.
 * No tenant schema resolution is needed.
 */
@Repository
@RequiredArgsConstructor
public class TenantViewRepositoryAdapter implements TenantViewRepository {
    private final TenantJpaRepository jpaRepository;
    private final TenantViewMapper mapper;

    @Override
    public Optional<TenantView> findById(TenantId tenantId) {
        Optional<TenantEntity> entity = jpaRepository.findById(tenantId.getValue());
        return entity.map(mapper::toView);
    }

    @Override
    public TenantListResult listTenants(TenantListQuery query) {
        // Use unsorted Pageable for native query - sorting is handled in the SQL query itself
        Pageable pageable = PageRequest.of(query.getPage() - 1, query.getSize());

        // Convert TenantStatus enum to String for native query to avoid PostgreSQL type inference issues
        String statusString = query.getStatus().map(Enum::name).orElse(null);

        Page<TenantEntity> page = jpaRepository.searchTenants(statusString, query.getSearch().map(String::toLowerCase).orElse(null), pageable);

        List<TenantView> tenants = page.getContent().stream().map(mapper::toView).collect(Collectors.toUnmodifiableList());

        return new TenantListResult(tenants, query.getPage(), query.getSize(), page.getTotalElements());
    }
}

