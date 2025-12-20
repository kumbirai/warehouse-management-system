package com.ccbsa.wms.user.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.user.application.service.port.repository.UserRepository;
import com.ccbsa.wms.user.application.service.query.dto.GetUserQueryResult;
import com.ccbsa.wms.user.application.service.query.dto.ListUsersQuery;
import com.ccbsa.wms.user.application.service.query.dto.ListUsersQueryResult;
import com.ccbsa.wms.user.domain.core.entity.User;
import com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId;

/**
 * Query Handler: ListUsersQueryHandler
 * <p>
 * Handles query for list of users with filtering.
 * <p>
 * Note: For MVP, using repository directly. In production, use read model (data port).
 */
@Component
public class ListUsersQueryHandler {
    private static final Logger logger = LoggerFactory.getLogger(ListUsersQueryHandler.class);

    private final UserRepository userRepository;

    public ListUsersQueryHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Handles the ListUsersQuery.
     * <p>
     * Read-only transaction for query optimization.
     *
     * @param query Query to execute
     * @return Query result with pagination
     */
    @Transactional(readOnly = true)
    public ListUsersQueryResult handle(ListUsersQuery query) {
        logger.debug("Listing users: tenantId={}, status={}, page={}, size={}, search={}", query.getTenantId(), query.getStatus(), query.getPage(), query.getSize(),
                query.getSearch());

        // 1. Load users from repository
        List<User> users;
        if (query.getTenantId() != null) {
            // Filter by tenant ID (single tenant query)
            String searchTerm = query.getSearch();
            boolean hasSearch = searchTerm != null && !searchTerm.trim().isEmpty();

            if (hasSearch && searchTerm != null) {
                // Apply search filter
                String trimmedSearchTerm = searchTerm.trim();
                if (query.getStatus() != null) {
                    users = userRepository.findByTenantIdAndStatusAndSearchTerm(query.getTenantId(), query.getStatus(), trimmedSearchTerm);
                } else {
                    users = userRepository.findByTenantIdAndSearchTerm(query.getTenantId(), trimmedSearchTerm);
                }
                logger.debug("Found {} users for tenantId={} with search term '{}'", users.size(), query.getTenantId(), trimmedSearchTerm);
            } else {
                // No search filter
                if (query.getStatus() != null) {
                    users = userRepository.findByTenantIdAndStatus(query.getTenantId(), query.getStatus());
                } else {
                    users = userRepository.findByTenantId(query.getTenantId());
                }
                logger.debug("Found {} users for tenantId={}", users.size(), query.getTenantId());
            }
        } else {
            // SYSTEM_ADMIN can query all users across all tenant schemas (tenantId is null)
            // Note: This is only reached if user has SYSTEM_ADMIN role (enforced by @PreAuthorize)
            String searchTerm = query.getSearch();
            boolean hasSearch = searchTerm != null && !searchTerm.trim().isEmpty();

            if (hasSearch && searchTerm != null) {
                // Apply search filter across all tenants
                String trimmedSearchTerm = searchTerm.trim();
                users = userRepository.findAllAcrossTenantsWithSearch(query.getStatus(), trimmedSearchTerm);
                logger.debug("Found {} users across all tenants with search term '{}' (SYSTEM_ADMIN query)", users.size(), trimmedSearchTerm);
            } else {
                // No search filter - query all users across all tenants
                users = userRepository.findAllAcrossTenants(query.getStatus());
                logger.debug("Found {} users across all tenants (SYSTEM_ADMIN query)", users.size());
            }
        }

        // 2. Apply pagination
        int start = query.getPage() * query.getSize();
        int end = Math.min(start + query.getSize(), users.size());
        List<User> paginatedUsers = users.subList(Math.min(start, users.size()), end);
        logger.debug("Returning {} users (page {} of {}, total: {})", paginatedUsers.size(), query.getPage(), (users.size() + query.getSize() - 1) / query.getSize(), users.size());

        // 3. Map to query results
        List<GetUserQueryResult> results = paginatedUsers.stream().map(this::toQueryResult).collect(Collectors.toList());

        // 4. Return result with pagination info
        return new ListUsersQueryResult(results, users.size(), query.getPage(), query.getSize());
    }

    private GetUserQueryResult toQueryResult(User user) {
        return new GetUserQueryResult(user.getId(), user.getTenantId(), null, // tenantName not fetched in list query for performance
                user.getUsername().getValue(), user.getEmail().getValue(), user.getFirstName().map(fn -> fn.getValue()).orElse(null),
                user.getLastName().map(ln -> ln.getValue()).orElse(null), user.getStatus(), user.getKeycloakUserId().map(KeycloakUserId::getValue).orElse(null),
                List.of(), // Roles would be fetched separately if needed
                user.getCreatedAt(), user.getLastModifiedAt());
    }
}

