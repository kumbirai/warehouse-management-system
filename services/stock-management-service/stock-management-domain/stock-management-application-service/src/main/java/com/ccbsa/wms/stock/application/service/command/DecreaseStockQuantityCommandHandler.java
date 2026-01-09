package com.ccbsa.wms.stock.application.service.command;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.stock.application.service.command.dto.DecreaseStockQuantityCommand;
import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: DecreaseStockQuantityCommandHandler
 * <p>
 * Handles decreasing stock quantity after picking task completion.
 * <p>
 * Responsibilities:
 * - Find stock items by product and location
 * - Decrease quantity using FEFO (First Expired First Out)
 * - Update stock items
 * - Check stock level thresholds after update
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DecreaseStockQuantityCommandHandler {

    private final StockItemRepository stockItemRepository;
    private final ProductServicePort productServicePort;

    @Transactional
    public void handle(DecreaseStockQuantityCommand command) {
        log.info("Decreasing stock quantity: productCode={}, locationId={}, quantity={}, tenantId={}", command.getProductCode(), command.getLocationId().getValueAsString(),
                command.getQuantity(), command.getTenantId().getValue());

        // 1. Get ProductId from productCode
        com.ccbsa.wms.product.domain.core.valueobject.ProductCode productCodeValue = com.ccbsa.wms.product.domain.core.valueobject.ProductCode.of(command.getProductCode());
        ProductServicePort.ProductInfo productInfo = productServicePort.getProductByCode(productCodeValue, command.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found for code: " + command.getProductCode()));
        ProductId productId = ProductId.of(productInfo.getProductId());

        // 2. Find stock items at the location
        List<StockItem> stockItems = stockItemRepository.findByTenantIdAndProductIdAndLocationId(command.getTenantId(), productId, command.getLocationId());

        if (stockItems.isEmpty()) {
            throw new IllegalStateException(
                    String.format("No stock items found for product: %s at location: %s", command.getProductCode(), command.getLocationId().getValueAsString()));
        }

        // 3. Decrease quantity using FEFO (already sorted by expiration in repository)
        int remainingDecrease = command.getQuantity();

        for (StockItem stockItem : stockItems) {
            if (remainingDecrease <= 0) {
                break;
            }

            // Skip expired stock (should not happen, but safety check)
            if (!stockItem.canBePicked()) {
                continue;
            }

            Quantity availableQuantity = stockItem.getAvailableQuantity();
            if (availableQuantity.getValue() <= 0) {
                continue;
            }

            int decreaseFromItem = Math.min(remainingDecrease, availableQuantity.getValue());
            stockItem.decreaseQuantity(Quantity.of(decreaseFromItem));
            stockItemRepository.save(stockItem);

            remainingDecrease -= decreaseFromItem;

            log.debug("Decreased stock item quantity: stockItemId={}, decreaseAmount={}, remainingDecrease={}, newQuantity={}", stockItem.getId().getValueAsString(),
                    decreaseFromItem, remainingDecrease, stockItem.getQuantity().getValue());
        }

        if (remainingDecrease > 0) {
            throw new IllegalStateException(
                    String.format("Insufficient stock for decrease. Requested: %d, Decreased: %d, Remaining: %d", command.getQuantity(), command.getQuantity() - remainingDecrease,
                            remainingDecrease));
        }

        log.info("Stock quantity decreased successfully: productCode={}, locationId={}, quantity={}", command.getProductCode(), command.getLocationId().getValueAsString(),
                command.getQuantity());
    }
}
