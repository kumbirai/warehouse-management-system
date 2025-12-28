package com.ccbsa.wms.user.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.user.application.service.port.data.UserViewRepository;
import com.ccbsa.wms.user.application.service.port.data.dto.UserView;
import com.ccbsa.wms.user.application.service.query.dto.GetUserQueryResult;
import com.ccbsa.wms.user.application.service.query.dto.ListUsersQuery;
import com.ccbsa.wms.user.application.service.query.dto.ListUsersQueryResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: ListUsersQueryHandler
 * <p>
 * Handles query for list of user read models with filtering.
 * <p>
 * Uses data port (UserViewRepository) instead of repository port for CQRS compliance.
 * <p>
 * Supports cross-tenant queries for SYSTEM_ADMIN users (when tenantId is null).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ListUsersQueryHandler {
    private final UserViewRepository viewRepository;

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
        log.debug("Listing users: tenantId={}, status={}, page={}, size={}, search={}", query.getTenantId(), query.getStatus(), query.getPage(), query.getSize(),
                query.getSearch());

        // 1. Load user views from data port
        List<UserView> userViews;
        if (query.getTenantId() != null) {
            // Filter by tenant ID (single tenant query)
            userViews = viewRepository.findByTenantId(query.getTenantId(), query.getStatus(), query.getSearch());
            log.debug("Found {} user views for tenantId={}", userViews.size(), query.getTenantId());
        } else {
            // SYSTEM_ADMIN can query all users across all tenant schemas (tenantId is null)
            // Note: This is only reached if user has SYSTEM_ADMIN role (enforced by @PreAuthorize)
            userViews = viewRepository.findAllAcrossTenants(query.getStatus(), query.getSearch());
            log.debug("Found {} user views across all tenants (SYSTEM_ADMIN query)", userViews.size());
        }

        // 2. Apply pagination
        int start = query.getPage() * query.getSize();
        int end = Math.min(start + query.getSize(), userViews.size());
        List<UserView> paginatedViews = userViews.subList(Math.min(start, userViews.size()), end);
        log.debug("Returning {} user views (page {} of {}, total: {})", paginatedViews.size(), query.getPage(), (userViews.size() + query.getSize() - 1) / query.getSize(),
                userViews.size());

        // 3. Map views to query results
        List<GetUserQueryResult> results = paginatedViews.stream().map(this::toQueryResult).collect(Collectors.toList());

        // 4. Return result with pagination info
        return new ListUsersQueryResult(results, userViews.size(), query.getPage(), query.getSize());
    }

    private GetUserQueryResult toQueryResult(UserView view) {
        return new GetUserQueryResult(view.getUserId(), view.getTenantId(), null, // tenantName not fetched in list query for performance
                view.getUsername(), view.getEmail(), view.getFirstName(), view.getLastName(), view.getStatus(), view.getKeycloakUserId(),
                List.of(), // Roles would be fetched separately if needed
                view.getCreatedAt(), view.getLastModifiedAt());
    }
}

