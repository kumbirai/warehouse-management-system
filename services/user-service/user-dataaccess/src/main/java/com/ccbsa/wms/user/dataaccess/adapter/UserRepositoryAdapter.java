package com.ccbsa.wms.user.dataaccess.adapter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.user.application.service.port.repository.UserRepository;
import com.ccbsa.wms.user.dataaccess.entity.UserEntity;
import com.ccbsa.wms.user.dataaccess.jpa.UserJpaRepository;
import com.ccbsa.wms.user.dataaccess.mapper.UserEntityMapper;
import com.ccbsa.wms.user.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.user.domain.core.entity.User;
import com.ccbsa.wms.user.domain.core.valueobject.FirstName;
import com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId;
import com.ccbsa.wms.user.domain.core.valueobject.LastName;
import com.ccbsa.wms.user.domain.core.valueobject.UserStatus;
import com.ccbsa.wms.user.domain.core.valueobject.Username;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Repository Adapter: UserRepositoryAdapter
 * <p>
 * Implements UserRepository port interface. Adapts between domain User aggregate and JPA UserEntity.
 */
@Repository
public class UserRepositoryAdapter implements UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepositoryAdapter.class);

    private final UserJpaRepository jpaRepository;
    private final UserEntityMapper mapper;
    private final JdbcTemplate jdbcTemplate;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    public UserRepositoryAdapter(UserJpaRepository jpaRepository, UserEntityMapper mapper, JdbcTemplate jdbcTemplate, TenantSchemaResolver schemaResolver,
                                 TenantSchemaProvisioner schemaProvisioner) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
        this.schemaResolver = schemaResolver;
        this.schemaProvisioner = schemaProvisioner;
    }

    @Override
    public void save(User user) {
        // Verify TenantContext is set before saving (critical for schema resolution)
        com.ccbsa.common.domain.valueobject.TenantId tenantId = com.ccbsa.wms.common.security.TenantContext.getTenantId();
        if (tenantId == null) {
            logger.error("TenantContext is not set when saving user! User will be saved to wrong schema. User tenantId: {}",
                    user.getTenantId() != null ? user.getTenantId().getValue() : "null");
            throw new IllegalStateException(
                    String.format("TenantContext must be set before saving user. Expected tenantId: %s", user.getTenantId() != null ? user.getTenantId().getValue() : "null"));
        }

        // Verify tenantId matches
        if (!tenantId.getValue().equals(user.getTenantId().getValue())) {
            logger.error("TenantContext mismatch! Context: {}, User: {}", tenantId.getValue(), user.getTenantId().getValue());
            throw new IllegalStateException("TenantContext tenantId does not match user tenantId");
        }

        logger.info("Saving user with TenantContext set to: {}", tenantId.getValue());

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.info("Resolved schema name: '{}' for tenantId: '{}'", schemaName, tenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        // This ensures Hibernate uses the correct schema even if naming strategy caching occurs
        // Note: This sets the search_path for the current transaction
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

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

        logger.info("User saved successfully to schema: '{}'", schemaName);
    }

    /**
     * Validates that a schema name matches expected patterns to prevent SQL injection.
     * <p>
     * Schema names must match one of these patterns: - 'public' (for legacy users) - 'tenant_*_schema' (for tenant-specific schemas)
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
        // Pattern: starts with 'tenant_', ends with '_schema', contains only alphanumeric and underscores
        if (schemaName.matches("^tenant_[a-zA-Z0-9_]+_schema$")) {
            return;
        }

        throw new IllegalArgumentException(String.format("Invalid schema name format: '%s'. Expected 'public' or 'tenant_*_schema' pattern", schemaName));
    }

    /**
     * Sets the PostgreSQL search_path for the current database connection.
     * <p>
     * This method sets the search_path to the specified schema name. The schema name must be validated before calling this method.
     *
     * @param session    Hibernate session
     * @param schemaName Validated schema name
     */
    private void setSearchPath(Session session, String schemaName) {
        session.doWork(connection -> executeSetSearchPath(connection, schemaName));
    }

    /**
     * Updates an existing JPA entity from domain object. Preserves JPA managed state and version for optimistic locking.
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
     * Escapes a PostgreSQL identifier to prevent SQL injection.
     * <p>
     * PostgreSQL identifiers should be quoted if they contain special characters or are case-sensitive. For our schema names (which follow a pattern), we can safely quote them.
     *
     * @param identifier The identifier to escape
     * @return Escaped identifier
     */
    private String escapeIdentifier(String identifier) {
        // Quote the identifier to handle any special characters
        // Replace any existing quotes to prevent injection
        return String.format("\"%s\"", identifier.replace("\"", "\"\""));
    }

    @Override
    public Optional<User> findById(UserId userId) {
        // Verify TenantContext is set (critical for schema resolution)
        com.ccbsa.common.domain.valueobject.TenantId contextTenantId = com.ccbsa.wms.common.security.TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when querying user by ID! Cannot resolve schema. UserId: {}", userId != null ? userId.getValue() : "null");
            throw new IllegalStateException("TenantContext must be set before querying user by ID");
        }

        logger.debug("Querying user by ID with TenantContext set to: {}", contextTenantId.getValue());

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        // This ensures Hibernate uses the correct schema even if naming strategy caching occurs
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Now query using JPA repository (will use the schema set in search_path)
        return jpaRepository.findById(userId.getValue()).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByTenantIdAndId(TenantId tenantId, UserId userId) {
        return jpaRepository.findByTenantIdAndUserId(tenantId.getValue(), userId.getValue()).map(mapper::toDomain);
    }

    @Override
    public List<User> findByTenantId(TenantId tenantId) {
        // Verify TenantContext is set (critical for schema resolution)
        com.ccbsa.common.domain.valueobject.TenantId contextTenantId = com.ccbsa.wms.common.security.TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when querying users! Cannot resolve schema. Requested tenantId: {}", tenantId != null ? tenantId.getValue() : "null");
            throw new IllegalStateException(
                    String.format("TenantContext must be set before querying users. Expected tenantId: %s", tenantId != null ? tenantId.getValue() : "null"));
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        logger.debug("Querying users with TenantContext set to: {}", contextTenantId.getValue());

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        // This ensures Hibernate uses the correct schema even if naming strategy caching occurs
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Now query using JPA repository (will use the schema set in search_path)
        return jpaRepository.findByTenantId(tenantId.getValue()).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return jpaRepository.findByUsername(username.getValue()).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByTenantIdAndUsername(TenantId tenantId, Username username) {
        return jpaRepository.findByTenantIdAndUsername(tenantId.getValue(), username.getValue()).map(mapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<User> findAllAcrossTenants(UserStatus status) {
        logger.info("Finding all users across all tenant schemas with status={}", status);

        try {
            // 1. Get all tenant schemas from information_schema using JdbcTemplate (bypasses Hibernate)
            List<String> tenantSchemas = getTenantSchemas();
            logger.info("Found {} tenant schemas: {}", tenantSchemas.size(), tenantSchemas);

            if (tenantSchemas.isEmpty()) {
                logger.warn("No tenant schemas found - returning empty list");
                return List.of();
            }

            // 2. Build UNION query dynamically
            String sql = buildCrossSchemaQuery(tenantSchemas, status);
            logger.info("Generated cross-schema SQL query (length={})", sql.length());
            logger.info("SQL query: {}", sql);

            // 3. Execute query using JdbcTemplate (bypasses Hibernate naming strategy)
            // Note: When status filter is applied, we need to pass the status value for each UNION part
            List<UserEntity> entities;
            if (status != null) {
                // For UNION queries with WHERE clauses, we need to pass the parameter for each UNION part
                // Since each SELECT has a WHERE status = ?, we need to pass status.name() multiple times
                Object[] params = new Object[tenantSchemas.size()];
                for (int i = 0; i < tenantSchemas.size(); i++) {
                    params[i] = status.name();
                }
                entities = jdbcTemplate.query(sql, new UserEntityRowMapper(), params);
            } else {
                entities = jdbcTemplate.query(sql, new UserEntityRowMapper());
            }

            logger.info("Retrieved {} users from all tenant schemas", entities.size());

            // 4. Map to domain objects
            return entities.stream().map(mapper::toDomain).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error executing cross-schema query: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to query users across tenant schemas", e);
        }
    }

    /**
     * Retrieves all tenant schemas from the database.
     * <p>
     * Queries information_schema.schemata to find all schemas matching the pattern 'tenant_%_schema', and also includes the 'public' schema (for legacy users that may have been
     * created before schema-per-tenant was fully implemented). Uses
     * JdbcTemplate to bypass Hibernate's naming strategy.
     *
     * @return List of schema names (tenant schemas + public schema)
     */
    private List<String> getTenantSchemas() {
        String sql = """
                SELECT schema_name
                FROM information_schema.schemata
                WHERE schema_name LIKE 'tenant_%_schema'
                   OR schema_name = 'public'
                ORDER BY schema_name
                """;

        return jdbcTemplate.queryForList(sql, String.class);
    }

    /**
     * Builds a UNION query to query users from all tenant schemas.
     * <p>
     * The query structure: SELECT user_id, tenant_id, username, email_address, first_name, last_name, keycloak_user_id, status, created_at, last_modified_at, version FROM
     * tenant_schema1.users UNION ALL SELECT user_id, tenant_id, username,
     * email_address, first_name, last_name, keycloak_user_id, status, created_at, last_modified_at, version FROM tenant_schema2.users ... WHERE status = ? (if status filter is
     * provided - using ? for JdbcTemplate)
     *
     * @param schemas List of tenant schema names
     * @param status  Optional status filter
     * @return SQL query string
     */
    private String buildCrossSchemaQuery(List<String> schemas, UserStatus status) {
        if (schemas.isEmpty()) {
            throw new IllegalArgumentException("At least one schema is required");
        }

        // Explicitly list all columns to ensure proper JPA entity mapping
        String columns =
                String.format("%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s", "user_id", "tenant_id", "username", "email_address", "first_name", "last_name", "keycloak_user_id",
                        "status", "created_at", "last_modified_at", "version");

        StringBuilder sql = new StringBuilder();
        List<String> unionParts = new ArrayList<>();

        // Build UNION ALL parts for each schema
        for (String schema : schemas) {
            // Escape schema name to prevent SQL injection
            String escapedSchema = escapeIdentifier(schema);
            if (status != null) {
                // Apply status filter to each SELECT statement using ? placeholder for JdbcTemplate
                unionParts.add(String.format("SELECT %s FROM %s.users WHERE status = ?", columns, escapedSchema));
            } else {
                unionParts.add(String.format("SELECT %s FROM %s.users", columns, escapedSchema));
            }
        }

        // Combine with UNION ALL
        sql.append(String.join(" UNION ALL ", unionParts));

        return sql.toString();
    }

    @Override
    public List<User> findAllAcrossTenantsWithSearch(UserStatus status, String searchTerm) {
        logger.info("Finding all users across all tenant schemas with status={} and searchTerm={}", status, searchTerm);

        try {
            // 1. Get all tenant schemas from information_schema using JdbcTemplate (bypasses Hibernate)
            List<String> tenantSchemas = getTenantSchemas();
            logger.info("Found {} tenant schemas: {}", tenantSchemas.size(), tenantSchemas);

            if (tenantSchemas.isEmpty()) {
                logger.warn("No tenant schemas found - returning empty list");
                return List.of();
            }

            // 2. Build UNION query dynamically with search
            String sql = buildCrossSchemaQueryWithSearch(tenantSchemas, status);
            logger.info("Generated cross-schema SQL query with search (length={})", sql.length());
            logger.debug("SQL query: {}", sql);

            // 3. Execute query using JdbcTemplate (bypasses Hibernate naming strategy)
            // We need to pass parameters for each UNION part: status (if provided) and searchTerm (twice for username and email)
            String searchPattern = String.format("%%%s%%", searchTerm.toLowerCase());
            List<UserEntity> entities;
            if (status != null) {
                // For UNION queries: status, username search, email search for each UNION part
                Object[] params = new Object[tenantSchemas.size() * 3];
                for (int i = 0; i < tenantSchemas.size(); i++) {
                    params[i * 3] = status.name();
                    params[i * 3 + 1] = searchPattern;
                    params[i * 3 + 2] = searchPattern;
                }
                entities = jdbcTemplate.query(sql, new UserEntityRowMapper(), params);
            } else {
                // Only searchTerm parameters (username and email) for each UNION part
                Object[] params = new Object[tenantSchemas.size() * 2];
                for (int i = 0; i < tenantSchemas.size(); i++) {
                    params[i * 2] = searchPattern;
                    params[i * 2 + 1] = searchPattern;
                }
                entities = jdbcTemplate.query(sql, new UserEntityRowMapper(), params);
            }

            logger.info("Retrieved {} users from all tenant schemas with search term '{}'", entities.size(), searchTerm);

            // 4. Map to domain objects
            return entities.stream().map(mapper::toDomain).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error executing cross-schema query with search: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to query users across tenant schemas with search", e);
        }
    }

    /**
     * Builds a cross-schema SQL query with search filter.
     * <p>
     * Creates a UNION ALL query that searches across all tenant schemas for users matching the search term
     * in username or email (case-insensitive).
     *
     * @param schemas List of tenant schema names
     * @param status  Optional user status filter
     * @return SQL query string with placeholders for status and searchTerm
     */
    private String buildCrossSchemaQueryWithSearch(List<String> schemas, UserStatus status) {
        if (schemas.isEmpty()) {
            throw new IllegalArgumentException("At least one schema is required");
        }

        // Explicitly list all columns to ensure proper JPA entity mapping
        String columns =
                String.format("%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s", "user_id", "tenant_id", "username", "email_address", "first_name", "last_name", "keycloak_user_id",
                        "status", "created_at", "last_modified_at", "version");

        StringBuilder sql = new StringBuilder();
        List<String> unionParts = new ArrayList<>();

        // Build UNION ALL parts for each schema
        for (String schema : schemas) {
            // Escape schema name to prevent SQL injection
            String escapedSchema = escapeIdentifier(schema);
            if (status != null) {
                // Apply status and search filters to each SELECT statement
                // Using ILIKE for case-insensitive search in PostgreSQL
                unionParts.add(String.format("SELECT %s FROM %s.users WHERE status = ? AND (LOWER(username) LIKE ? OR LOWER(email_address) LIKE ?)", columns, escapedSchema));
            } else {
                // Only search filter
                unionParts.add(String.format("SELECT %s FROM %s.users WHERE LOWER(username) LIKE ? OR LOWER(email_address) LIKE ?", columns, escapedSchema));
            }
        }

        // Combine with UNION ALL
        sql.append(String.join(" UNION ALL ", unionParts));

        return sql.toString();
    }

    @Override
    public Optional<User> findByIdAcrossTenants(com.ccbsa.common.domain.valueobject.UserId userId) {
        logger.debug("Finding user by ID across all tenant schemas: userId={}", userId.getValue());

        try {
            // 1. Get all tenant schemas
            List<String> tenantSchemas = getTenantSchemas();
            if (tenantSchemas.isEmpty()) {
                logger.warn("No tenant schemas found - returning empty");
                return Optional.empty();
            }

            // 2. Build UNION query with WHERE user_id = ?
            String columns =
                    String.format("%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s", "user_id", "tenant_id", "username", "email_address", "first_name", "last_name", "keycloak_user_id",
                            "status", "created_at", "last_modified_at", "version");

            List<String> unionParts = new ArrayList<>();
            for (String schema : tenantSchemas) {
                String escapedSchema = escapeIdentifier(schema);
                unionParts.add(String.format("SELECT %s FROM %s.users WHERE user_id = ?", columns, escapedSchema));
            }

            String sql = String.join(" UNION ALL ", unionParts);
            logger.debug("Generated cross-schema findById SQL query");

            // 3. Execute query - need to pass userId for each UNION part
            Object[] params = new Object[tenantSchemas.size()];
            for (int i = 0; i < tenantSchemas.size(); i++) {
                params[i] = userId.getValue();
            }

            List<UserEntity> entities = jdbcTemplate.query(sql, new UserEntityRowMapper(), params);

            if (entities.isEmpty()) {
                logger.debug("User not found across any tenant schema: userId={}", userId.getValue());
                return Optional.empty();
            }

            if (entities.size() > 1) {
                logger.warn("Found {} users with same ID across schemas (should not happen): userId={}", entities.size(), userId.getValue());
            }

            // 4. Map to domain object
            return Optional.of(mapper.toDomain(entities.get(0)));
        } catch (Exception e) {
            logger.error("Error finding user by ID across tenant schemas: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to find user across tenant schemas", e);
        }
    }

    @Override
    public List<User> findByTenantIdAndStatus(TenantId tenantId, UserStatus status) {
        // Verify TenantContext is set (critical for schema resolution)
        com.ccbsa.common.domain.valueobject.TenantId contextTenantId = com.ccbsa.wms.common.security.TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when querying users! Cannot resolve schema. Requested tenantId: {}", tenantId != null ? tenantId.getValue() : "null");
            throw new IllegalStateException(
                    String.format("TenantContext must be set before querying users. Expected tenantId: %s", tenantId != null ? tenantId.getValue() : "null"));
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        logger.debug("Querying users with status filter - TenantContext: {}, Status: {}", contextTenantId.getValue(), status);

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        // This ensures Hibernate uses the correct schema even if naming strategy caching occurs
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Now query using JPA repository (will use the schema set in search_path)
        UserEntity.UserStatus entityStatus = UserEntity.UserStatus.valueOf(status.name());
        return jpaRepository.findByTenantIdAndStatus(tenantId.getValue(), entityStatus).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<User> findByTenantIdAndSearchTerm(TenantId tenantId, String searchTerm) {
        // Verify TenantContext is set (critical for schema resolution)
        com.ccbsa.common.domain.valueobject.TenantId contextTenantId = com.ccbsa.wms.common.security.TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when querying users! Cannot resolve schema. Requested tenantId: {}", tenantId != null ? tenantId.getValue() : "null");
            throw new IllegalStateException(
                    String.format("TenantContext must be set before querying users. Expected tenantId: %s", tenantId != null ? tenantId.getValue() : "null"));
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        logger.debug("Querying users with search term - TenantContext: {}, SearchTerm: {}", contextTenantId.getValue(), searchTerm);

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        // This ensures Hibernate uses the correct schema even if naming strategy caching occurs
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Now query using JPA repository (will use the schema set in search_path)
        return jpaRepository.findByTenantIdAndSearchTerm(tenantId.getValue(), searchTerm).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<User> findByTenantIdAndStatusAndSearchTerm(TenantId tenantId, UserStatus status, String searchTerm) {
        // Verify TenantContext is set (critical for schema resolution)
        com.ccbsa.common.domain.valueobject.TenantId contextTenantId = com.ccbsa.wms.common.security.TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when querying users! Cannot resolve schema. Requested tenantId: {}", tenantId != null ? tenantId.getValue() : "null");
            throw new IllegalStateException(
                    String.format("TenantContext must be set before querying users. Expected tenantId: %s", tenantId != null ? tenantId.getValue() : "null"));
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        logger.debug("Querying users with status and search term - TenantContext: {}, Status: {}, SearchTerm: {}", contextTenantId.getValue(), status, searchTerm);

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        // This ensures Hibernate uses the correct schema even if naming strategy caching occurs
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Now query using JPA repository (will use the schema set in search_path)
        UserEntity.UserStatus entityStatus = UserEntity.UserStatus.valueOf(status.name());
        return jpaRepository.findByTenantIdAndStatusAndSearchTerm(tenantId.getValue(), entityStatus, searchTerm).stream().map(mapper::toDomain).collect(Collectors.toList());
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
        return jpaRepository.findByKeycloakUserId(keycloakUserId).map(mapper::toDomain);
    }

    /**
     * RowMapper for UserEntity to map ResultSet rows to UserEntity objects.
     */
    private static class UserEntityRowMapper implements RowMapper<UserEntity> {
        @Override
        public UserEntity mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            UserEntity entity = new UserEntity();
            entity.setUserId(rs.getString("user_id"));
            entity.setTenantId(rs.getString("tenant_id"));
            entity.setUsername(rs.getString("username"));
            entity.setEmailAddress(rs.getString("email_address"));
            entity.setFirstName(rs.getString("first_name"));
            entity.setLastName(rs.getString("last_name"));
            entity.setKeycloakUserId(rs.getString("keycloak_user_id"));

            String statusStr = rs.getString("status");
            if (statusStr != null) {
                entity.setStatus(UserEntity.UserStatus.valueOf(statusStr));
            }

            entity.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
            entity.setLastModifiedAt(rs.getTimestamp("last_modified_at") != null ? rs.getTimestamp("last_modified_at").toLocalDateTime() : null);
            entity.setVersion(rs.getLong("version"));

            return entity;
        }
    }
}

