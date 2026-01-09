package com.ccbsa.wms.stock.application.service.query;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.wms.stock.application.service.port.data.StockItemViewRepository;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockItemView;
import com.ccbsa.wms.stock.application.service.query.dto.CheckStockExpirationQuery;
import com.ccbsa.wms.stock.application.service.query.dto.CheckStockExpirationQueryResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: CheckStockExpirationQueryHandler
 * <p>
 * Handles checking if stock is expired at a specific location.
 * <p>
 * Responsibilities:
 * - Query stock items from read model by product and location
 * - Check if any stock items are expired
 * - Return expiration status and details
 * <p>
 * Uses data port (StockItemViewRepository) for CQRS compliance.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CheckStockExpirationQueryHandler {

    private final StockItemViewRepository viewRepository;

    @Transactional(readOnly = true)
    public CheckStockExpirationQueryResult handle(CheckStockExpirationQuery query) {
        log.debug("Handling CheckStockExpirationQuery for product: {}, location: {}, tenant: {}", query.getProductId().getValueAsString(), query.getLocationId().getValueAsString(),
                query.getTenantId().getValue());

        // 1. Query stock items from read model
        List<StockItemView> stockItemViews = viewRepository.findByTenantIdAndProductIdAndLocationId(query.getTenantId(), query.getProductId(), query.getLocationId());

        if (stockItemViews.isEmpty()) {
            log.debug("No stock items found for product: {} at location: {}", query.getProductId().getValueAsString(), query.getLocationId().getValueAsString());
            return CheckStockExpirationQueryResult.builder().expired(false).expirationDate(null).classification(StockClassification.NORMAL).daysUntilExpiration(Integer.MAX_VALUE)
                    .message("No stock found at this location").build();
        }

        // 2. Check if any stock items are expired
        // Use the first stock item with expiration date (FEFO - earliest first)
        StockItemView stockItem = stockItemViews.stream().filter(item -> item.getExpirationDate() != null).findFirst().orElse(stockItemViews.get(0));

        LocalDate expirationDate = stockItem.getExpirationDate() != null ? stockItem.getExpirationDate().getValue() : null;
        StockClassification classification = stockItem.getClassification();

        // 3. Calculate days until expiration and determine if expired
        int daysUntilExpiration = Integer.MAX_VALUE;
        boolean expired = false;
        if (expirationDate != null) {
            LocalDate today = LocalDate.now();
            long days = ChronoUnit.DAYS.between(today, expirationDate);
            daysUntilExpiration = (int) days;
            // Check expiration date directly, not just classification (classification might not be updated in view yet)
            expired = daysUntilExpiration < 0;
            // If expired by date but classification not updated, use EXPIRED classification
            if (expired && classification != StockClassification.EXPIRED) {
                classification = StockClassification.EXPIRED;
            }
        } else {
            // No expiration date - use classification from view
            expired = classification == StockClassification.EXPIRED;
        }

        // 4. Build message
        String message;
        if (expired) {
            message = String.format("Stock expired on %s", expirationDate);
        } else if (expirationDate != null) {
            if (daysUntilExpiration < 0) {
                message = String.format("Stock expired %d days ago", Math.abs(daysUntilExpiration));
            } else if (daysUntilExpiration <= 7) {
                message = String.format("Stock expires in %d days (CRITICAL)", daysUntilExpiration);
            } else if (daysUntilExpiration <= 30) {
                message = String.format("Stock expires in %d days (NEAR_EXPIRY)", daysUntilExpiration);
            } else {
                message = String.format("Stock expires in %d days", daysUntilExpiration);
            }
        } else {
            message = "Stock has no expiration date";
        }

        log.debug("Stock expiration check result: expired={}, classification={}, daysUntilExpiration={}", expired, classification, daysUntilExpiration);

        return CheckStockExpirationQueryResult.builder().expired(expired).expirationDate(expirationDate).classification(classification).daysUntilExpiration(daysUntilExpiration)
                .message(message).build();
    }
}
