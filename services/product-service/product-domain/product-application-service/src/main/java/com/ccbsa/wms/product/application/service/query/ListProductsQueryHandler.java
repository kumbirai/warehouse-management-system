package com.ccbsa.wms.product.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.product.application.service.port.repository.ProductRepository;
import com.ccbsa.wms.product.application.service.query.dto.ListProductsQuery;
import com.ccbsa.wms.product.application.service.query.dto.ListProductsQueryResult;
import com.ccbsa.wms.product.application.service.query.dto.ProductQueryResult;
import com.ccbsa.wms.product.domain.core.entity.Product;

/**
 * Query Handler: ListProductsQueryHandler
 * <p>
 * Handles listing of Product aggregates with optional filtering.
 * <p>
 * Responsibilities:
 * - Load Product aggregates from repository
 * - Apply filters (category, brand, search)
 * - Map aggregates to query result DTOs
 * - Return paginated results
 */
@Component
public class ListProductsQueryHandler {

    private final ProductRepository repository;

    public ListProductsQueryHandler(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ListProductsQueryResult handle(ListProductsQuery query) {
        // 1. Normalize pagination parameters
        int page = query.getPage() != null ? query.getPage() : 0;
        int size = query.getSize() != null ? query.getSize() : 100;

        // 2. Query products with database-level filtering and pagination
        // This is much more efficient than loading all products into memory
        List<Product> paginatedProducts = repository.findByTenantIdWithFilters(query.getTenantId(), query.getCategory(), query.getBrand(), query.getSearch(), page, size);

        // 3. Get total count for pagination metadata
        long totalCount = repository.countByTenantIdWithFilters(query.getTenantId(), query.getCategory(), query.getBrand(), query.getSearch());

        // 4. Map to query results
        List<ProductQueryResult> productResults = paginatedProducts.stream().map(this::toProductQueryResult).collect(Collectors.toList());

        // 5. Build result with pagination metadata
        return ListProductsQueryResult.builder().products(productResults).totalCount((int) totalCount).page(page).size(size).build();
    }

    private ProductQueryResult toProductQueryResult(Product product) {
        return ProductQueryResult.builder().productId(product.getId()).productCode(product.getProductCode()).description(product.getDescription().getValue())
                .primaryBarcode(product.getPrimaryBarcode()).secondaryBarcodes(product.getSecondaryBarcodes()).unitOfMeasure(product.getUnitOfMeasure())
                .category(product.getCategory() != null ? product.getCategory().getValue() : null).brand(product.getBrand() != null ? product.getBrand().getValue() : null)
                .createdAt(product.getCreatedAt()).lastModifiedAt(product.getLastModifiedAt()).build();
    }
}

