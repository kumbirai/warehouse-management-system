package com.ccbsa.wms.notification.dataaccess.adapter;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.cache.decorator.CachedRepositoryDecorator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.notification.application.service.port.repository.NotificationRepository;
import com.ccbsa.wms.notification.domain.core.entity.Notification;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationId;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationStatus;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Cached Notification Repository Adapter.
 * <p>
 * MANDATORY: Decorates NotificationRepositoryAdapter with Redis caching.
 * Implements cache-aside pattern for reads and write-through for writes.
 * <p>
 * Cache Configuration:
 * - Namespace: "notifications"
 * - TTL: Configured via application.yml (default: 30 minutes)
 * - Eviction: LRU (configured in Redis)
 * <p>
 * Spring Configuration:
 * - @Primary: Ensures this adapter is injected instead of base adapter
 * - @Repository: Marks as Spring Data repository component
 */
@Repository
@Primary
public class CachedNotificationRepositoryAdapter extends CachedRepositoryDecorator<Notification, NotificationId> implements NotificationRepository {

    private static final Logger log = LoggerFactory.getLogger(CachedNotificationRepositoryAdapter.class);

    private final NotificationRepositoryAdapter baseRepository;

    public CachedNotificationRepositoryAdapter(NotificationRepositoryAdapter baseRepository, RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        super(baseRepository, redisTemplate, CacheNamespace.NOTIFICATIONS.getValue(), Duration.ofMinutes(30), // TTL from config
                meterRegistry);
        this.baseRepository = baseRepository;
    }

    @Override
    public Notification save(Notification notification) {
        // Write-through: Save to database + update cache
        Notification saved = baseRepository.save(notification);

        if (saved.getTenantId() != null && saved.getId() != null) {
            saveWithCache(saved.getTenantId(), saved.getId().getValue(), saved, obj -> saved // Already saved
            );
        }

        return saved;
    }

    @Override
    public Optional<Notification> findById(NotificationId notificationId) {
        // findById without tenantId - need to check if Notification is tenant-aware
        // Since NotificationRepository.findByRecipientUserId requires tenantId, 
        // we should use tenant context for caching
        TenantId tenantId = com.ccbsa.wms.common.security.TenantContext.getTenantId();
        if (tenantId != null) {
            // Use tenant-aware caching
            return findWithCache(tenantId, notificationId.getValue(), entityId -> baseRepository.findById(notificationId));
        }
        // Fallback to base repository if no tenant context
        return baseRepository.findById(notificationId);
    }

    @Override
    public List<Notification> findByRecipientUserId(TenantId tenantId, UserId recipientUserId) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByRecipientUserId(tenantId, recipientUserId);
    }

    @Override
    public List<Notification> findByRecipientUserIdAndStatus(TenantId tenantId, UserId recipientUserId, NotificationStatus status) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByRecipientUserIdAndStatus(tenantId, recipientUserId, status);
    }

    @Override
    public List<Notification> findByType(TenantId tenantId, NotificationType type) {
        // Collections NOT cached to avoid cache bloat
        return baseRepository.findByType(tenantId, type);
    }

    @Override
    public long countUnreadByRecipientUserId(TenantId tenantId, UserId recipientUserId) {
        // Count queries are dynamic, not cached
        return baseRepository.countUnreadByRecipientUserId(tenantId, recipientUserId);
    }
}

