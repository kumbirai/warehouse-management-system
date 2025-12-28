package com.ccbsa.wms.product.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.product.application.service.port.data.ProductViewRepository;
import com.ccbsa.wms.product.application.service.port.data.dto.ProductView;
import com.ccbsa.wms.product.application.service.query.dto.ListProductsQuery;
import com.ccbsa.wms.product.application.service.query.dto.ListProductsQueryResult;
import com.ccbsa.wms.product.application.service.query.dto.ProductQueryResult;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;

/**
 * Query Handler: ListProductsQueryHandler
 * <p>
 * Handles listing of Product read models with optional filtering.
 * <p>
 * Responsibilities:
 * - Load Product views from data port (read model)
 * - Apply filters (category, brand, search)
 * - Map views to query result DTOs
 * - Return paginated results
 * <p>
 * Uses data port (ProductViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@RequiredArgsConstructor
@SuppressFBWarnings(value = {"DLS_DEAD_LOCAL_STORE", "UPM_UNCALLED_PRIVATE_METHOD"}, justification =
        "DLS_DEAD_LOCAL_STORE: Variables are used in builder - SpotBugs false positive. "
                + "UPM_UNCALLED_PRIVATE_METHOD: Method called via method reference - SpotBugs false positive.")
public class ListProductsQueryHandler {
    private final ProductViewRepository viewRepository;

    @Transactional(readOnly = true)
    public ListProductsQueryResult handle(ListProductsQuery query) {
        // 1. Normalize pagination parameters
        int page = query.getPage() != null ? query.getPage() : 0;
        int size = query.getSize() != null ? query.getSize() : 100;

        // 2. Query product views with database-level filtering and pagination
        // This is much more efficient than loading all products into memory
        List<ProductView> paginatedViews = viewRepository.findByTenantIdWithFilters(query.getTenantId(), query.getCategory(), query.getBrand(), query.getSearch(), page, size);

        // 3. Get total count for pagination metadata
        long totalCount = viewRepository.countByTenantIdWithFilters(query.getTenantId(), query.getCategory(), query.getBrand(), query.getSearch());

        // 4. Map views to query results
        List<ProductQueryResult> productResults = paginatedViews.stream().map(this::toProductQueryResult).collect(Collectors.toList());

        // 5. Build result with pagination metadata
        return ListProductsQueryResult.builder().products(productResults).totalCount((int) totalCount).page(page).size(size).build();
    }

    private ProductQueryResult toProductQueryResult(ProductView view) {
        return ProductQueryResult.builder().productId(view.getProductId()).productCode(view.getProductCode()).description(view.getDescription())
                .primaryBarcode(view.getPrimaryBarcode()).secondaryBarcodes(view.getSecondaryBarcodes()).unitOfMeasure(view.getUnitOfMeasure()).category(view.getCategory())
                .brand(view.getBrand()).createdAt(view.getCreatedAt()).lastModifiedAt(view.getLastModifiedAt()).build();
    }
}

