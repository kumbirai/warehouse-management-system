package com.ccbsa.wms.product.messaging.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ccbsa.common.cache.invalidation.CacheInvalidationEventListener;
import com.ccbsa.common.cache.invalidation.LocalCacheInvalidator;
import com.ccbsa.common.cache.key.CacheNamespace;
import com.ccbsa.wms.product.domain.core.event.ProductCreatedEvent;
import com.ccbsa.wms.product.domain.core.event.ProductUpdatedEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Product Service Cache Invalidation Listener.
 * <p>
 * MANDATORY: Listens to product domain events and invalidates affected caches.
 * <p>
 * Invalidation Strategy:
 * - ProductCreatedEvent: No invalidation (cache-aside pattern)
 * - ProductUpdatedEvent: Invalidate entity + collections
 */
@Slf4j
@Component
public class ProductCacheInvalidationListener extends CacheInvalidationEventListener {
    public ProductCacheInvalidationListener(LocalCacheInvalidator cacheInvalidator) {
        super(cacheInvalidator);
    }

    @KafkaListener(topics = "product-events", groupId = "product-service-cache-invalidation", containerFactory = "externalEventKafkaListenerContainerFactory")
    public void handleProductEvent(Object event) {
        if (event instanceof ProductCreatedEvent) {
            // No invalidation - cache-aside pattern
            log.debug("Product created, no cache invalidation needed");
        } else if (event instanceof ProductUpdatedEvent updated) {
            // Invalidate entity cache and all collection caches
            invalidateForEvent(updated, CacheNamespace.PRODUCTS.getValue());
        }
    }
}

