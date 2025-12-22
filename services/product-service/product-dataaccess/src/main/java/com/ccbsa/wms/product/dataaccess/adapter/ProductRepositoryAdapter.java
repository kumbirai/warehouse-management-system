package com.ccbsa.wms.product.dataaccess.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.product.application.service.port.repository.ProductRepository;
import com.ccbsa.wms.product.dataaccess.entity.ProductBarcodeEntity;
import com.ccbsa.wms.product.dataaccess.entity.ProductEntity;
import com.ccbsa.wms.product.dataaccess.jpa.ProductBarcodeJpaRepository;
import com.ccbsa.wms.product.dataaccess.jpa.ProductJpaRepository;
import com.ccbsa.wms.product.dataaccess.mapper.ProductEntityMapper;
import com.ccbsa.wms.product.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.product.domain.core.entity.Product;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Repository Adapter: ProductRepositoryAdapter
 * <p>
 * Implements ProductRepository port interface. Adapts between domain Product aggregate and JPA ProductEntity.
 */
@Repository
public class ProductRepositoryAdapter implements ProductRepository {
    private static final Logger logger = LoggerFactory.getLogger(ProductRepositoryAdapter.class);

    private final ProductJpaRepository jpaRepository;
    private final ProductBarcodeJpaRepository barcodeJpaRepository;
    private final ProductEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    public ProductRepositoryAdapter(ProductJpaRepository jpaRepository, ProductBarcodeJpaRepository barcodeJpaRepository, ProductEntityMapper mapper,
                                    TenantSchemaResolver schemaResolver, TenantSchemaProvisioner schemaProvisioner) {
        this.jpaRepository = jpaRepository;
        this.barcodeJpaRepository = barcodeJpaRepository;
        this.mapper = mapper;
        this.schemaResolver = schemaResolver;
        this.schemaProvisioner = schemaProvisioner;
    }

    @Override
    public Product save(Product product) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            logger.error("TenantContext is not set when saving product! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before saving product");
        }

        // Verify tenantId matches
        if (!tenantId.getValue().equals(product.getTenantId().getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Product: {}", tenantId.getValue(), product.getTenantId().getValue());
            throw new IllegalStateException("TenantContext tenantId does not match product tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, tenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Check if entity already exists to handle version correctly
        Optional<ProductEntity> existingEntity = jpaRepository.findByTenantIdAndId(product.getTenantId().getValue(), product.getId().getValue());

        ProductEntity entity;
        if (existingEntity.isPresent()) {
            // Update existing entity - preserve JPA managed state and version
            entity = existingEntity.get();
            updateEntityFromDomain(entity, product);
        } else {
            // New entity - create from domain model
            entity = mapper.toEntity(product);
        }

        ProductEntity savedEntity = jpaRepository.save(entity);
        Product savedProduct = mapper.toDomain(savedEntity);

        logger.debug("Product saved successfully to schema: '{}'", schemaName);

        // Domain events are preserved by the command handler before calling save()
        // The command handler gets domain events from the original product before save()
        // and publishes them after transaction commit. We return the saved product
        // which may not have events, but that's OK since events are already captured.
        return savedProduct;
    }

    /**
     * Validates that a schema name matches expected patterns to prevent SQL injection.
     * <p>
     * Schema names must match one of these patterns:
     * - 'public' (for legacy data)
     * - 'tenant_*_schema' (for tenant-specific schemas)
     *
     * @param schemaName The schema name to validate
     * @throws IllegalArgumentException if schema name does not match expected patterns
     */
    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema name cannot be null or empty");
        }

        // Allow 'public' schema
        if ("public".equals(schemaName)) {
            return;
        }

        // Allow tenant schemas matching pattern: tenant_*_schema
        if (schemaName.matches("^tenant_[a-zA-Z0-9_]+_schema$")) {
            return;
        }

        throw new IllegalArgumentException(String.format("Invalid schema name format: '%s'. Expected 'public' or 'tenant_*_schema' pattern", schemaName));
    }

    /**
     * Sets the PostgreSQL search_path for the current database connection.
     * <p>
     * This method sets the search_path to the specified schema name.
     * The schema name must be validated before calling this method.
     *
     * @param session    Hibernate session
     * @param schemaName Validated schema name
     */
    private void setSearchPath(Session session, String schemaName) {
        session.doWork(connection -> executeSetSearchPath(connection, schemaName));
    }

    /**
     * Updates an existing entity with values from the domain model. Preserves JPA managed state and version for optimistic locking.
     *
     * @param entity  Existing JPA entity
     * @param product Domain product aggregate
     */
    private void updateEntityFromDomain(ProductEntity entity, Product product) {
        entity.setProductCode(product.getProductCode().getValue());
        entity.setDescription(product.getDescription().getValue());
        entity.setPrimaryBarcode(product.getPrimaryBarcode().getValue());
        entity.setPrimaryBarcodeType(product.getPrimaryBarcode().getType());
        entity.setUnitOfMeasure(product.getUnitOfMeasure());
        entity.setCategory(product.getCategory() != null ? product.getCategory().getValue() : null);
        entity.setBrand(product.getBrand() != null ? product.getBrand().getValue() : null);
        entity.setLastModifiedAt(product.getLastModifiedAt());

        // Update secondary barcodes - remove all existing and add new ones
        entity.getSecondaryBarcodes().clear();
        for (ProductBarcode barcode : product.getSecondaryBarcodes()) {
            ProductBarcodeEntity barcodeEntity = new ProductBarcodeEntity();
            barcodeEntity.setId(UUID.randomUUID());
            barcodeEntity.setProduct(entity);
            barcodeEntity.setBarcode(barcode.getValue());
            barcodeEntity.setBarcodeType(barcode.getType());
            entity.getSecondaryBarcodes().add(barcodeEntity);
        }

        // Version is managed by JPA - don't update it manually
    }

    /**
     * Executes the SET search_path SQL command.
     * <p>
     * This method is separated to allow proper SpotBugs annotation placement.
     *
     * @param connection Database connection
     * @param schemaName Validated and escaped schema name
     */
    @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Schema name is validated against expected patterns (tenant_*_schema or public) "
            + "and properly escaped using escapeIdentifier() method. " + "PostgreSQL SET search_path command does not support parameterized queries.")
    private void executeSetSearchPath(Connection connection, String schemaName) {
        try (Statement stmt = connection.createStatement()) {
            String setSchemaSql = String.format("SET search_path TO %s", escapeIdentifier(schemaName));
            logger.debug("Setting search_path to: {}", schemaName);
            stmt.execute(setSchemaSql);
        } catch (SQLException e) {
            logger.error("Failed to set search_path to schema '{}': {}", schemaName, e.getMessage(), e);
            throw new RuntimeException("Failed to set database schema", e);
        }
    }

    /**
     * Escapes a PostgreSQL identifier to prevent SQL injection.
     * <p>
     * PostgreSQL identifiers are case-sensitive when quoted. This method wraps
     * the identifier in double quotes and escapes any internal quotes.
     *
     * @param identifier The identifier to escape
     * @return Escaped identifier safe for use in SQL
     */
    private String escapeIdentifier(String identifier) {
        // PostgreSQL identifiers: wrap in double quotes and escape internal quotes
        return String.format("\"%s\"", identifier.replace("\"", "\"\""));
    }

    @Override
    public Optional<Product> findByIdAndTenantId(ProductId id, TenantId tenantId) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when querying product! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying product");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Now query using JPA repository (will use the schema set in search_path)
        return jpaRepository.findByTenantIdAndId(tenantId.getValue(), id.getValue()).map(mapper::toDomain);
    }

    @Override
    public Optional<Product> findByProductCodeAndTenantId(ProductCode productCode, TenantId tenantId) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when querying product! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying product");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Now query using JPA repository (will use the schema set in search_path)
        return jpaRepository.findByTenantIdAndProductCode(tenantId.getValue(), productCode.getValue()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByProductCodeAndTenantId(ProductCode productCode, TenantId tenantId) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when checking product existence! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before checking product existence");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Now query using JPA repository (will use the schema set in search_path)
        return jpaRepository.existsByTenantIdAndProductCode(tenantId.getValue(), productCode.getValue());
    }

    @Override
    public boolean existsByBarcodeAndTenantId(ProductBarcode barcode, TenantId tenantId) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when checking barcode existence! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before checking barcode existence");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Check primary barcode
        if (jpaRepository.existsByTenantIdAndPrimaryBarcode(tenantId.getValue(), barcode.getValue())) {
            return true;
        }
        // Check secondary barcodes
        return barcodeJpaRepository.existsByBarcode(barcode.getValue());
    }

    @Override
    public boolean existsByBarcodeAndTenantIdExcludingProduct(ProductBarcode barcode, TenantId tenantId, ProductId excludeProductId) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when checking barcode existence! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before checking barcode existence");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Check primary barcode (excluding the specified product)
        if (jpaRepository.existsByTenantIdAndPrimaryBarcodeAndIdNot(tenantId.getValue(), barcode.getValue(), excludeProductId.getValue())) {
            return true;
        }
        // Check secondary barcodes (excluding the specified product)
        return barcodeJpaRepository.existsByBarcodeAndTenantIdExcludingProduct(barcode.getValue(), tenantId.getValue(), excludeProductId.getValue());
    }

    @Override
    public Optional<Product> findByBarcodeAndTenantId(String barcode, TenantId tenantId) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when querying product by barcode! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying product by barcode");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // First check primary barcode
        Optional<ProductEntity> primaryBarcodeProduct = jpaRepository.findByTenantIdAndPrimaryBarcode(tenantId.getValue(), barcode);
        if (primaryBarcodeProduct.isPresent()) {
            return primaryBarcodeProduct.map(mapper::toDomain);
        }

        // Then check secondary barcodes
        Optional<ProductBarcodeEntity> secondaryBarcode = barcodeJpaRepository.findByBarcodeAndTenantId(barcode, tenantId.getValue());
        if (secondaryBarcode.isPresent()) {
            ProductEntity productEntity = secondaryBarcode.get().getProduct();
            return Optional.of(mapper.toDomain(productEntity));
        }

        return Optional.empty();
    }

    @Override
    public List<Product> findByTenantId(TenantId tenantId) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when querying products! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying products");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Now query using JPA repository (will use the schema set in search_path)
        return jpaRepository.findByTenantId(tenantId.getValue()).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Product> findByTenantIdAndCategory(TenantId tenantId, String category) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when querying products by category! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying products by category");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Now query using JPA repository (will use the schema set in search_path)
        return jpaRepository.findByTenantIdAndCategory(tenantId.getValue(), category).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Product> findByTenantIdWithFilters(TenantId tenantId, String category, String brand, String search, int page, int size) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when querying products with filters! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying products with filters");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Normalize filter values (null for empty strings)
        String normalizedCategory = (category != null && !category.trim().isEmpty()) ? category.trim() : null;
        String normalizedBrand = (brand != null && !brand.trim().isEmpty()) ? brand.trim() : null;
        String normalizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // Create Pageable for pagination
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        // Query using JPA repository with database-level filtering
        // Returns Page to enable automatic pagination by Spring Data JPA
        org.springframework.data.domain.Page<ProductEntity> productPage =
                jpaRepository.findByTenantIdWithFilters(tenantId.getValue(), normalizedCategory, normalizedBrand, normalizedSearch, pageable);

        return productPage.getContent().stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countByTenantIdWithFilters(TenantId tenantId, String category, String brand, String search) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when counting products with filters! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before counting products with filters");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Normalize filter values (null for empty strings)
        String normalizedCategory = (category != null && !category.trim().isEmpty()) ? category.trim() : null;
        String normalizedBrand = (brand != null && !brand.trim().isEmpty()) ? brand.trim() : null;
        String normalizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // Count using JPA repository with database-level filtering
        return jpaRepository.countByTenantIdWithFilters(tenantId.getValue(), normalizedCategory, normalizedBrand, normalizedSearch);
    }
}

