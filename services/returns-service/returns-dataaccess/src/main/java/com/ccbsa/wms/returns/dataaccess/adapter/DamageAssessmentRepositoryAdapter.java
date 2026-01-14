package com.ccbsa.wms.returns.dataaccess.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.returns.application.service.port.repository.DamageAssessmentRepository;
import com.ccbsa.wms.returns.dataaccess.entity.DamageAssessmentEntity;
import com.ccbsa.wms.returns.dataaccess.jpa.DamageAssessmentJpaRepository;
import com.ccbsa.wms.returns.dataaccess.mapper.DamageAssessmentEntityMapper;
import com.ccbsa.wms.returns.domain.core.entity.DamageAssessment;
import com.ccbsa.wms.returns.domain.core.valueobject.DamageAssessmentId;
import com.ccbsa.wms.returns.domain.core.valueobject.InsuranceClaimInfo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: DamageAssessmentRepositoryAdapter
 * <p>
 * Implements DamageAssessmentRepository port interface. Adapts between domain DamageAssessment aggregate and JPA DamageAssessmentEntity.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class DamageAssessmentRepositoryAdapter implements DamageAssessmentRepository {
    private final DamageAssessmentJpaRepository jpaRepository;
    private final DamageAssessmentEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public DamageAssessment save(DamageAssessment damageAssessment) {
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.error("TenantContext is not set when saving damage assessment! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before saving damage assessment");
        }

        if (!tenantId.getValue().equals(damageAssessment.getTenantId().getValue())) {
            log.error("TenantContext mismatch! Context: {}, DamageAssessment: {}", tenantId.getValue(), damageAssessment.getTenantId().getValue());
            throw new IllegalStateException("TenantContext tenantId does not match damage assessment tenantId");
        }

        String schemaName = schemaResolver.resolveSchema();
        log.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, tenantId.getValue());

        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        Optional<DamageAssessmentEntity> existingEntity =
                jpaRepository.findByTenantIdAndAssessmentId(damageAssessment.getTenantId().getValue(), damageAssessment.getId().getValue());

        DamageAssessmentEntity entity;
        if (existingEntity.isPresent()) {
            entity = existingEntity.get();
            updateEntityFromDomain(entity, damageAssessment);
        } else {
            entity = mapper.toEntity(damageAssessment);
        }

        jpaRepository.save(entity);
        log.debug("Damage assessment saved successfully to schema: '{}'", schemaName);

        return mapper.toDomain(entity);
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema name cannot be null or empty");
        }
        if ("public".equals(schemaName)) {
            return;
        }
        if (schemaName.matches("^tenant_[a-zA-Z0-9_]+_schema$")) {
            return;
        }
        throw new IllegalArgumentException(String.format("Invalid schema name format: '%s'", schemaName));
    }

    private void setSearchPath(Session session, String schemaName) {
        session.doWork(connection -> executeSetSearchPath(connection, schemaName));
    }

    private void updateEntityFromDomain(DamageAssessmentEntity entity, DamageAssessment damageAssessment) {
        entity.setOrderNumber(damageAssessment.getOrderNumber().getValue());
        entity.setDamageType(damageAssessment.getDamageType());
        entity.setDamageSeverity(damageAssessment.getDamageSeverity());
        entity.setDamageSource(damageAssessment.getDamageSource());
        entity.setAssessmentStatus(damageAssessment.getStatus());
        entity.setDamageNotes(damageAssessment.getDamageNotes() != null ? damageAssessment.getDamageNotes().getValue() : null);
        entity.setRecordedAt(damageAssessment.getRecordedAt());
        entity.setLastModifiedAt(damageAssessment.getLastModifiedAt());

        if (damageAssessment.getInsuranceClaimInfo() != null) {
            InsuranceClaimInfo claimInfo = damageAssessment.getInsuranceClaimInfo();
            entity.setInsuranceClaimNumber(claimInfo.getClaimNumber());
            entity.setInsuranceCompany(claimInfo.getInsuranceCompany());
            entity.setInsuranceClaimStatus(claimInfo.getClaimStatus());
            entity.setInsuranceClaimAmount(claimInfo.getClaimAmount());
        }

        entity.getDamagedProductItems().clear();
        DamageAssessmentEntity newEntity = mapper.toEntity(damageAssessment);
        entity.getDamagedProductItems().addAll(newEntity.getDamagedProductItems());
        entity.getDamagedProductItems().forEach(item -> item.setDamageAssessment(entity));
    }

    @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Schema name is validated and properly escaped")
    private void executeSetSearchPath(Connection connection, String schemaName) {
        try (Statement stmt = connection.createStatement()) {
            String setSchemaSql = String.format("SET search_path TO %s", escapeIdentifier(schemaName));
            log.debug("Setting search_path to: {}", schemaName);
            stmt.execute(setSchemaSql);
        } catch (SQLException e) {
            log.error("Failed to set search_path to schema '{}': {}", schemaName, e.getMessage(), e);
            throw new RuntimeException("Failed to set database schema", e);
        }
    }

    private String escapeIdentifier(String identifier) {
        return String.format("\"%s\"", identifier.replace("\"", "\"\""));
    }

    @Override
    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "Calling size() to force initialization of lazy-loaded JPA collection")
    public Optional<DamageAssessment> findByIdAndTenantId(DamageAssessmentId id, TenantId tenantId) {
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying damage assessment! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying damage assessment");
        }

        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            log.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        String schemaName = schemaResolver.resolveSchema();
        log.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        jakarta.persistence.Query query = entityManager.createQuery(
                "SELECT d FROM DamageAssessmentEntity d LEFT JOIN FETCH d.damagedProductItems WHERE d.tenantId = :tenantId AND d.assessmentId = :assessmentId",
                DamageAssessmentEntity.class);
        query.setParameter("tenantId", tenantId.getValue());
        query.setParameter("assessmentId", id.getValue());

        @SuppressWarnings("unchecked") java.util.List<DamageAssessmentEntity> results = query.getResultList();

        if (results.isEmpty()) {
            return Optional.empty();
        }

        DamageAssessmentEntity entity = results.get(0);
        if (entity.getDamagedProductItems() != null) {
            entity.getDamagedProductItems().size();
        }

        return Optional.of(mapper.toDomain(entity));
    }
}
