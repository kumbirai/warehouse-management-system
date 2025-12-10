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
        logger.debug("Listing users: tenantId={}, status={}, page={}, size={}",
                query.getTenantId(), query.getStatus(), query.getPage(), query.getSize());

        // 1. Load users from repository
        List<User> users;
        if (query.getTenantId() != null) {
            // Filter by tenant ID (single tenant query)
            if (query.getStatus() != null) {
                users = userRepository.findByTenantIdAndStatus(query.getTenantId(), query.getStatus());
            } else {
                users = userRepository.findByTenantId(query.getTenantId());
            }
        } else {
            // SYSTEM_ADMIN can query all users across all tenant schemas (tenantId is null)
            // Note: This is only reached if user has SYSTEM_ADMIN role (enforced by @PreAuthorize)
            // Use findAllAcrossTenants to query across all tenant schemas
            users = userRepository.findAllAcrossTenants(query.getStatus());
        }

        // 2. Apply pagination
        int start = query.getPage() * query.getSize();
        int end = Math.min(start + query.getSize(), users.size());
        List<User> paginatedUsers = users.subList(Math.min(start, users.size()), end);

        // 3. Map to query results
        List<GetUserQueryResult> results = paginatedUsers.stream()
                .map(this::toQueryResult)
                .collect(Collectors.toList());

        // 4. Return result with pagination info
        return new ListUsersQueryResult(results, users.size(), query.getPage(), query.getSize());
    }

    private GetUserQueryResult toQueryResult(User user) {
        return new GetUserQueryResult(
                user.getId(),
                user.getTenantId(),
                user.getUsername().getValue(),
                user.getEmail().getValue(),
                user.getFirstName().map(fn -> fn.getValue()).orElse(null),
                user.getLastName().map(ln -> ln.getValue()).orElse(null),
                user.getStatus(),
                user.getKeycloakUserId().map(KeycloakUserId::getValue).orElse(null),
                List.of(), // Roles would be fetched separately if needed
                user.getCreatedAt(),
                user.getLastModifiedAt());
    }
}

