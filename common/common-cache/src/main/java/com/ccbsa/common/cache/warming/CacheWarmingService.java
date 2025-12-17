package com.ccbsa.common.cache.warming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Cache Warming Service.
 * <p>
 * Warms up critical caches on application startup to reduce initial response latency.
 * <p>
 * Warming Strategy: 1. Triggered by ApplicationReadyEvent (all beans initialized) 2. Runs asynchronously to not block startup 3. Warms only critical, frequently accessed data 4.
 * Logs warming progress and errors
 * <p>
 * Service-specific implementations extend this base class.
 */
@Service
public abstract class CacheWarmingService {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmingService.class);

    /**
     * Triggered when application is fully started and ready to accept requests.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async("cacheWarmingExecutor")
    public void warmCacheOnStartup() {
        log.info("Starting cache warming...");

        try {
            performCacheWarming();
            log.info("Cache warming completed successfully");
        } catch (Exception e) {
            log.error("Cache warming failed", e);
            // Don't throw - warming failure shouldn't prevent startup
        }
    }

    /**
     * Implement this method to define service-specific cache warming logic.
     */
    protected abstract void performCacheWarming();
}
