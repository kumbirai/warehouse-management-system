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
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.user.application.service.port.data.UserViewRepository;
import com.ccbsa.wms.user.application.service.port.data.dto.UserView;
import com.ccbsa.wms.user.dataaccess.entity.UserViewEntity;
import com.ccbsa.wms.user.dataaccess.jpa.UserViewJpaRepository;
import com.ccbsa.wms.user.dataaccess.mapper.UserViewEntityMapper;
import com.ccbsa.wms.user.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.user.domain.core.valueobject.UserStatus;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: UserViewRepositoryAdapter
 * <p>
 * Implements UserViewRepository data port interface. Provides read model access to user data.
 * <p>
 * This adapter handles tenant schema resolution and search_path setting for multi-tenant isolation.
 * It also supports cross-tenant queries for SYSTEM_ADMIN users using JdbcTemplate.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class UserViewRepositoryAdapter implements UserViewRepository {
    private final UserViewJpaRepository jpaRepository;
    private final UserViewEntityMapper mapper;
    private final JdbcTemplate jdbcTemplate;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<UserView> findById(UserId userId) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying user view!");
            throw new IllegalStateException("TenantContext must be set before querying user view");
        }

        // Resolve schema and set search_path
        String schemaName = schemaResolver.resolveSchema();
        log.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Query view entity
        Optional<UserViewEntity> entity = jpaRepository.findByUserId(userId.getValue());
        return entity.map(mapper::toView);
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
    public Optional<UserView> findByIdAcrossTenants(UserId userId) {
        log.debug("Finding user view by ID across all tenant schemas: userId={}", userId.getValue());

        try {
            // 1. Get all tenant schemas
            List<String> tenantSchemas = getTenantSchemas();
            if (tenantSchemas.isEmpty()) {
                log.warn("No tenant schemas found - returning empty");
                return Optional.empty();
            }

            // 2. Build UNION query with WHERE user_id = ?
            String columns = "user_id, tenant_id, username, email_address, first_name, last_name, keycloak_user_id, status, created_at, last_modified_at";

            List<String> unionParts = new ArrayList<>();
            for (String schema : tenantSchemas) {
                String escapedSchema = escapeIdentifier(schema);
                unionParts.add(String.format("SELECT %s FROM %s.users WHERE user_id = ?", columns, escapedSchema));
            }

            String sql = String.join(" UNION ALL ", unionParts);
            log.debug("Generated cross-schema findById SQL query");

            // 3. Execute query - need to pass userId for each UNION part
            Object[] params = new Object[tenantSchemas.size()];
            for (int i = 0; i < tenantSchemas.size(); i++) {
                params[i] = userId.getValue();
            }

            List<UserViewEntity> entities = jdbcTemplate.query(sql, new UserViewEntityRowMapper(), params);

            if (entities.isEmpty()) {
                log.debug("User not found across any tenant schema: userId={}", userId.getValue());
                return Optional.empty();
            }

            if (entities.size() > 1) {
                log.warn("Found {} users with same ID across schemas (should not happen): userId={}", entities.size(), userId.getValue());
            }

            // 4. Map to view DTO
            return Optional.of(mapper.toView(entities.get(0)));
        } catch (Exception e) {
            log.error("Error finding user view by ID across tenant schemas: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to find user view across tenant schemas", e);
        }
    }

    /**
     * Retrieves all tenant schemas from the database.
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

    @Override
    public List<UserView> findByTenantId(TenantId tenantId, UserStatus status, String search) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying user views!");
            throw new IllegalStateException("TenantContext must be set before querying user views");
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

        // Query based on filters
        List<UserViewEntity> entities;
        if (search != null && !search.trim().isEmpty()) {
            String trimmedSearch = search.trim();
            if (status != null) {
                entities = jpaRepository.findByTenantIdAndStatusAndSearchTerm(tenantId.getValue(), status, trimmedSearch);
            } else {
                entities = jpaRepository.findByTenantIdAndSearchTerm(tenantId.getValue(), trimmedSearch);
            }
        } else {
            if (status != null) {
                entities = jpaRepository.findByTenantIdAndStatus(tenantId.getValue(), status);
            } else {
                entities = jpaRepository.findByTenantId(tenantId.getValue());
            }
        }

        return entities.stream().map(mapper::toView).collect(Collectors.toList());
    }

    @Override
    public List<UserView> findAllAcrossTenants(UserStatus status, String search) {
        log.info("Finding all user views across all tenant schemas with status={}, search={}", status, search);

        try {
            // 1. Get all tenant schemas
            List<String> tenantSchemas = getTenantSchemas();
            log.info("Found {} tenant schemas: {}", tenantSchemas.size(), tenantSchemas);

            if (tenantSchemas.isEmpty()) {
                log.warn("No tenant schemas found - returning empty list");
                return List.of();
            }

            // 2. Build UNION query dynamically
            String sql;
            Object[] params;
            if (search != null && !search.trim().isEmpty()) {
                sql = buildCrossSchemaQueryWithSearch(tenantSchemas, status);
                String searchPattern = String.format("%%%s%%", search.toLowerCase(java.util.Locale.ROOT));
                if (status != null) {
                    // For each UNION part: status + 4 search patterns (username, email, firstName, lastName)
                    params = new Object[tenantSchemas.size() * 5];
                    for (int i = 0; i < tenantSchemas.size(); i++) {
                        params[i * 5] = status.name();
                        params[i * 5 + 1] = searchPattern;
                        params[i * 5 + 2] = searchPattern;
                        params[i * 5 + 3] = searchPattern;
                        params[i * 5 + 4] = searchPattern;
                    }
                } else {
                    // For each UNION part: 4 search patterns (username, email, firstName, lastName)
                    params = new Object[tenantSchemas.size() * 4];
                    for (int i = 0; i < tenantSchemas.size(); i++) {
                        params[i * 4] = searchPattern;
                        params[i * 4 + 1] = searchPattern;
                        params[i * 4 + 2] = searchPattern;
                        params[i * 4 + 3] = searchPattern;
                    }
                }
            } else {
                sql = buildCrossSchemaQuery(tenantSchemas, status);
                if (status != null) {
                    params = new Object[tenantSchemas.size()];
                    for (int i = 0; i < tenantSchemas.size(); i++) {
                        params[i] = status.name();
                    }
                } else {
                    params = new Object[0];
                }
            }

            log.debug("Generated cross-schema SQL query");

            // 3. Execute query using JdbcTemplate
            List<UserViewEntity> entities = jdbcTemplate.query(sql, new UserViewEntityRowMapper(), params);

            log.info("Retrieved {} user views from all tenant schemas", entities.size());

            // 4. Map to view DTOs
            return entities.stream().map(mapper::toView).collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Error executing cross-schema query: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to query user views across tenant schemas", e);
        }
    }

    /**
     * Builds a cross-schema SQL query with search filter.
     */
    private String buildCrossSchemaQueryWithSearch(List<String> schemas, UserStatus status) {
        if (schemas.isEmpty()) {
            throw new IllegalArgumentException("At least one schema is required");
        }

        String columns = "user_id, tenant_id, username, email_address, first_name, last_name, keycloak_user_id, status, created_at, last_modified_at";

        List<String> unionParts = new ArrayList<>();
        for (String schema : schemas) {
            String escapedSchema = escapeIdentifier(schema);
            if (status != null) {
                // Status filter + 4 search patterns (username, email, firstName, lastName)
                unionParts.add(String.format(
                        "SELECT %s FROM %s.users WHERE status = ? AND (LOWER(username) LIKE ? OR LOWER(email_address) LIKE ? OR LOWER(COALESCE(first_name, '')) LIKE ? OR LOWER"
                                + "(COALESCE(last_name, '')) LIKE ?)", columns, escapedSchema));
            } else {
                // 4 search patterns (username, email, firstName, lastName)
                unionParts.add(String.format(
                        "SELECT %s FROM %s.users WHERE LOWER(username) LIKE ? OR LOWER(email_address) LIKE ? OR LOWER(COALESCE(first_name, '')) LIKE ? OR LOWER(COALESCE"
                                + "(last_name, '')) LIKE ?", columns, escapedSchema));
            }
        }

        return String.join(" UNION ALL ", unionParts);
    }

    /**
     * Builds a UNION query to query user views from all tenant schemas.
     */
    private String buildCrossSchemaQuery(List<String> schemas, UserStatus status) {
        if (schemas.isEmpty()) {
            throw new IllegalArgumentException("At least one schema is required");
        }

        String columns = "user_id, tenant_id, username, email_address, first_name, last_name, keycloak_user_id, status, created_at, last_modified_at";

        List<String> unionParts = new ArrayList<>();
        for (String schema : schemas) {
            String escapedSchema = escapeIdentifier(schema);
            if (status != null) {
                unionParts.add(String.format("SELECT %s FROM %s.users WHERE status = ?", columns, escapedSchema));
            } else {
                unionParts.add(String.format("SELECT %s FROM %s.users", columns, escapedSchema));
            }
        }

        return String.join(" UNION ALL ", unionParts);
    }

    /**
     * RowMapper for UserViewEntity to map ResultSet rows to UserViewEntity objects.
     */
    private static class UserViewEntityRowMapper implements RowMapper<UserViewEntity> {
        @Override
        public UserViewEntity mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            UserViewEntity entity = new UserViewEntity();
            entity.setUserId(rs.getString("user_id"));
            entity.setTenantId(rs.getString("tenant_id"));
            entity.setUsername(rs.getString("username"));
            entity.setEmailAddress(rs.getString("email_address"));
            entity.setFirstName(rs.getString("first_name"));
            entity.setLastName(rs.getString("last_name"));
            entity.setKeycloakUserId(rs.getString("keycloak_user_id"));

            String statusStr = rs.getString("status");
            if (statusStr != null) {
                entity.setStatus(UserStatus.valueOf(statusStr));
            }

            entity.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
            entity.setLastModifiedAt(rs.getTimestamp("last_modified_at") != null ? rs.getTimestamp("last_modified_at").toLocalDateTime() : null);

            return entity;
        }
    }
}

