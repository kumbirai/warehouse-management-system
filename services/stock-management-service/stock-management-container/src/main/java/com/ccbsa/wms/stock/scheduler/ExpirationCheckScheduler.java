package com.ccbsa.wms.stock.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ccbsa.wms.stock.application.service.command.CheckExpirationDatesCommandHandler;
import com.ccbsa.wms.stock.application.service.command.dto.CheckExpirationDatesCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler: ExpirationCheckScheduler
 * <p>
 * Scheduled job that runs daily to check stock expiration dates and update classifications.
 * <p>
 * Schedule: Runs daily at 2:00 AM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ExpirationCheckScheduler {

    private final CheckExpirationDatesCommandHandler checkExpirationDatesCommandHandler;

    /**
     * Scheduled task to check expiration dates for all stock items.
     * <p>
     * Runs daily at 2:00 AM (cron: "0 0 2 * * ?")
     * <p>
     * This job:
     * - Checks all stock items for expiration
     * - Updates classifications
     * - Publishes expiration events
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2:00 AM
    public void checkExpirationDates() {
        log.info("Starting scheduled expiration check for all stock items");

        try {
            // Note: Tenant context should be set by the scheduler or we need to process all tenants
            // For now, this will be called per-tenant or we need a multi-tenant scheduler
            // This is a simplified version - in production, you'd iterate over all tenants
            CheckExpirationDatesCommand command = CheckExpirationDatesCommand.builder().build();
            checkExpirationDatesCommandHandler.handle(command);

            log.info("Completed scheduled expiration check");
        } catch (Exception e) {
            log.error("Error during scheduled expiration check", e);
        }
    }
}
