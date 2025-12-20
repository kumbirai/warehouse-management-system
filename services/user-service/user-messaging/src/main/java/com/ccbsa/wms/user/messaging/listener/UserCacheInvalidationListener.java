package com.ccbsa.wms.user.messaging.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ccbsa.common.cache.invalidation.CacheInvalidationEventListener;
import com.ccbsa.common.cache.invalidation.LocalCacheInvalidator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.wms.user.domain.core.event.UserCreatedEvent;
import com.ccbsa.wms.user.domain.core.event.UserDeactivatedEvent;
import com.ccbsa.wms.user.domain.core.event.UserRoleAssignedEvent;
import com.ccbsa.wms.user.domain.core.event.UserRoleRemovedEvent;
import com.ccbsa.wms.user.domain.core.event.UserUpdatedEvent;

/**
 * User Service Cache Invalidation Listener.
 * <p>
 * Listens to user domain events and invalidates affected caches.
 * <p>
 * Invalidation Strategy: - UserCreatedEvent: No invalidation needed (cache-aside pattern) - UserUpdatedEvent: Invalidate user entity + all user collections - UserDeactivatedEvent:
 * Invalidate user entity + all user collections -
 * UserRoleAssignedEvent: Invalidate user roles + permissions - UserRoleRemovedEvent: Invalidate user roles + permissions
 */
@Component
public class UserCacheInvalidationListener extends CacheInvalidationEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserCacheInvalidationListener.class);

    public UserCacheInvalidationListener(LocalCacheInvalidator cacheInvalidator) {
        super(cacheInvalidator);
    }

    @KafkaListener(topics = "user-events", groupId = "user-service-cache-invalidation", containerFactory = "cacheInvalidationKafkaListenerContainerFactory")
    public void handleUserEvent(Object event) {
        if (event instanceof UserCreatedEvent userCreated) {
            handleUserCreated(userCreated);
        } else if (event instanceof UserUpdatedEvent userUpdated) {
            handleUserUpdated(userUpdated);
        } else if (event instanceof UserDeactivatedEvent userDeactivated) {
            handleUserDeactivated(userDeactivated);
        } else if (event instanceof UserRoleAssignedEvent roleAssigned) {
            handleRoleAssigned(roleAssigned);
        } else if (event instanceof UserRoleRemovedEvent roleRemoved) {
            handleRoleRemoved(roleRemoved);
        }
    }

    private void handleUserCreated(UserCreatedEvent event) {
        // No cache invalidation needed - cache-aside pattern
        // Cache will be populated on first read
        log.debug("User created, cache-aside pattern - no invalidation");
    }

    private void handleUserUpdated(UserUpdatedEvent event) {
        // Invalidate user entity cache and all user collection caches
        invalidateForEvent(event, CacheNamespace.USERS.getValue());
    }

    private void handleUserDeactivated(UserDeactivatedEvent event) {
        // Invalidate user entity cache and all user collection caches
        invalidateForEvent(event, CacheNamespace.USERS.getValue());
    }

    private void handleRoleAssigned(UserRoleAssignedEvent event) {
        // Invalidate user roles and permissions (cascade invalidation)
        invalidateForEvent(event, CacheNamespace.USERS.getValue(), CacheNamespace.USER_ROLES.getValue(), CacheNamespace.USER_PERMISSIONS.getValue());
    }

    private void handleRoleRemoved(UserRoleRemovedEvent event) {
        // Invalidate user roles and permissions (cascade invalidation)
        invalidateForEvent(event, CacheNamespace.USERS.getValue(), CacheNamespace.USER_ROLES.getValue(), CacheNamespace.USER_PERMISSIONS.getValue());
    }
}
