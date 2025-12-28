package com.ccbsa.wms.product.dataaccess.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.product.application.service.port.data.ProductViewRepository;
import com.ccbsa.wms.product.application.service.port.data.dto.ProductView;
import com.ccbsa.wms.product.dataaccess.entity.ProductBarcodeEntity;
import com.ccbsa.wms.product.dataaccess.entity.ProductViewEntity;
import com.ccbsa.wms.product.dataaccess.jpa.ProductBarcodeJpaRepository;
import com.ccbsa.wms.product.dataaccess.jpa.ProductViewJpaRepository;
import com.ccbsa.wms.product.dataaccess.mapper.ProductViewEntityMapper;
import com.ccbsa.wms.product.dataaccess.schema.TenantSchemaProvisioner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: ProductViewRepositoryAdapter
 * <p>
 * Implements ProductViewRepository data port interface. Provides read model access to product data.
 * <p>
 * This adapter handles tenant schema resolution and search_path setting for multi-tenant isolation.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class ProductViewRepositoryAdapter implements ProductViewRepository {
    private final ProductViewJpaRepository jpaRepository;
    private final ProductBarcodeJpaRepository barcodeJpaRepository;
    private final ProductViewEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<ProductView> findByTenantIdAndId(TenantId tenantId, ProductId productId) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying product view!");
            throw new IllegalStateException("TenantContext must be set before querying product view");
        }

        // Verify tenantId matches
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            log.error("TenantContext mismatch! Context: {}, Query: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match query tenantId");
        }

        // Resolve schema and set search_path
        String schemaName = schemaResolver.resolveSchema();
        log.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, tenantId.getValue());

        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Query view entity
        Optional<ProductViewEntity> entityOpt = jpaRepository.findByTenantIdAndId(tenantId.getValue(), productId.getValue());
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }

        ProductViewEntity entity = entityOpt.get();

        // Load secondary barcodes
        List<ProductBarcodeEntity> barcodeEntities = barcodeJpaRepository.findByProductId(entity.getId());

        // Map to view DTO
        return Optional.of(mapper.toView(entity, barcodeEntities));
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema name cannot be null or empty");
        }
        if ("public".equals(schemaName)) {
            return;
        }
        if (!schemaName.matches("^tenant_[a-zA-Z0-9_]+_schema$")) {
            throw new IllegalArgumentException(String.format("Invalid schema name format: '%s'", schemaName));
        }
    }

    private void setSearchPath(Session session, String schemaName) {
        session.doWork(connection -> executeSetSearchPath(connection, schemaName));
    }

    @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Schema name validated against expected patterns and escaped")
    private void executeSetSearchPath(Connection connection, String schemaName) {
        try (Statement stmt = connection.createStatement()) {
            String sql = String.format("SET search_path TO %s", escapeIdentifier(schemaName));
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set database schema", e);
        }
    }

    private String escapeIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public Optional<ProductView> findByTenantIdAndProductCode(TenantId tenantId, String productCode) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying product view!");
            throw new IllegalStateException("TenantContext must be set before querying product view");
        }

        // Verify tenantId matches
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            log.error("TenantContext mismatch! Context: {}, Query: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match query tenantId");
        }

        // Resolve schema and set search_path
        String schemaName = schemaResolver.resolveSchema();
        log.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, tenantId.getValue());

        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Query view entity
        Optional<ProductViewEntity> entityOpt = jpaRepository.findByTenantIdAndProductCode(tenantId.getValue(), productCode);
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }

        ProductViewEntity entity = entityOpt.get();

        // Load secondary barcodes
        List<ProductBarcodeEntity> barcodeEntities = barcodeJpaRepository.findByProductId(entity.getId());

        // Map to view DTO
        return Optional.of(mapper.toView(entity, barcodeEntities));
    }

    @Override
    public List<ProductView> findByTenantId(TenantId tenantId) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying product views!");
            throw new IllegalStateException("TenantContext must be set before querying product views");
        }

        // Verify tenantId matches
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            log.error("TenantContext mismatch! Context: {}, Query: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match query tenantId");
        }

        // Resolve schema and set search_path
        String schemaName = schemaResolver.resolveSchema();
        log.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, tenantId.getValue());

        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Query view entities
        List<ProductViewEntity> entities = jpaRepository.findByTenantId(tenantId.getValue());

        // Load secondary barcodes for all products
        return entities.stream().map(entity -> {
            List<ProductBarcodeEntity> barcodeEntities = barcodeJpaRepository.findByProductId(entity.getId());
            return mapper.toView(entity, barcodeEntities);
        }).collect(Collectors.toList());
    }

    @Override
    public boolean existsByTenantIdAndProductCode(TenantId tenantId, String productCode) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when checking product code existence!");
            throw new IllegalStateException("TenantContext must be set before checking product code existence");
        }

        // Verify tenantId matches
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            log.error("TenantContext mismatch! Context: {}, Query: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match query tenantId");
        }

        // Resolve schema and set search_path
        String schemaName = schemaResolver.resolveSchema();
        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        return jpaRepository.existsByTenantIdAndProductCode(tenantId.getValue(), productCode);
    }

    @Override
    public boolean existsByTenantIdAndBarcode(TenantId tenantId, String barcode) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when checking barcode existence!");
            throw new IllegalStateException("TenantContext must be set before checking barcode existence");
        }

        // Verify tenantId matches
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            log.error("TenantContext mismatch! Context: {}, Query: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match query tenantId");
        }

        // Resolve schema and set search_path
        String schemaName = schemaResolver.resolveSchema();
        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Check primary barcode first (faster)
        if (jpaRepository.existsByTenantIdAndPrimaryBarcode(tenantId.getValue(), barcode)) {
            return true;
        }

        // Check secondary barcodes using the complex query
        return jpaRepository.existsByTenantIdAndBarcode(tenantId.getValue(), barcode);
    }

    @Override
    public Optional<ProductView> findByTenantIdAndBarcode(TenantId tenantId, String barcode) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying product view by barcode!");
            throw new IllegalStateException("TenantContext must be set before querying product view");
        }

        // Verify tenantId matches
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            log.error("TenantContext mismatch! Context: {}, Query: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match query tenantId");
        }

        // Resolve schema and set search_path
        String schemaName = schemaResolver.resolveSchema();
        log.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, tenantId.getValue());

        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Query view entity by barcode
        Optional<ProductViewEntity> entityOpt = jpaRepository.findByTenantIdAndBarcode(tenantId.getValue(), barcode);
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }

        ProductViewEntity entity = entityOpt.get();

        // Load secondary barcodes
        List<ProductBarcodeEntity> barcodeEntities = barcodeJpaRepository.findByProductId(entity.getId());

        // Map to view DTO
        return Optional.of(mapper.toView(entity, barcodeEntities));
    }

    @Override
    public List<ProductView> findByTenantIdWithFilters(TenantId tenantId, String category, String brand, String search, int page, int size) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying product views with filters!");
            throw new IllegalStateException("TenantContext must be set before querying product views");
        }

        // Verify tenantId matches
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            log.error("TenantContext mismatch! Context: {}, Query: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match query tenantId");
        }

        // Resolve schema and set search_path
        String schemaName = schemaResolver.resolveSchema();
        log.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, tenantId.getValue());

        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Create pageable
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        // Query view entities with filters
        org.springframework.data.domain.Page<ProductViewEntity> pageResult = jpaRepository.findByTenantIdWithFilters(tenantId.getValue(), category, brand, search, pageable);

        // Load secondary barcodes for all products and map to views
        return pageResult.getContent().stream().map(entity -> {
            List<ProductBarcodeEntity> barcodeEntities = barcodeJpaRepository.findByProductId(entity.getId());
            return mapper.toView(entity, barcodeEntities);
        }).collect(Collectors.toList());
    }

    @Override
    public long countByTenantIdWithFilters(TenantId tenantId, String category, String brand, String search) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when counting product views with filters!");
            throw new IllegalStateException("TenantContext must be set before counting product views");
        }

        // Verify tenantId matches
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            log.error("TenantContext mismatch! Context: {}, Query: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match query tenantId");
        }

        // Resolve schema and set search_path
        String schemaName = schemaResolver.resolveSchema();
        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        return jpaRepository.countByTenantIdWithFilters(tenantId.getValue(), category, brand, search);
    }
}

