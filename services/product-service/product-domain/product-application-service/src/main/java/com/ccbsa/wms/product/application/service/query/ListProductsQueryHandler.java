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
        // 1. Load all products for tenant
        List<Product> allProducts = repository.findByTenantId(query.getTenantId());

        // 2. Apply filters
        List<Product> filteredProducts =
                allProducts.stream().filter(product -> matchesCategory(product, query.getCategory())).filter(product -> matchesBrand(product, query.getBrand()))
                        .filter(product -> matchesSearch(product, query.getSearch())).collect(Collectors.toList());

        // 3. Apply pagination
        int page = query.getPage() != null ? query.getPage() : 0;
        int size = query.getSize() != null ? query.getSize() : 100;
        int start = page * size;
        int end = Math.min(start + size, filteredProducts.size());

        List<Product> paginatedProducts = filteredProducts.subList(Math.min(start, filteredProducts.size()), end);

        // 4. Map to query results
        List<ProductQueryResult> productResults = paginatedProducts.stream().map(this::toProductQueryResult).collect(Collectors.toList());

        // 5. Build result
        return ListProductsQueryResult.builder().products(productResults).totalCount(filteredProducts.size()).page(page).size(size).build();
    }

    private boolean matchesCategory(Product product, String category) {
        if (category == null || category.isBlank()) {
            return true;
        }
        return product.getCategory() != null && product.getCategory().equalsIgnoreCase(category);
    }

    private boolean matchesBrand(Product product, String brand) {
        if (brand == null || brand.isBlank()) {
            return true;
        }
        return product.getBrand() != null && product.getBrand().equalsIgnoreCase(brand);
    }

    private boolean matchesSearch(Product product, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String searchLower = search.toLowerCase();
        return (product.getProductCode().getValue().toLowerCase().contains(searchLower)) || (product.getDescription() != null && product.getDescription().toLowerCase()
                .contains(searchLower)) || (product.getPrimaryBarcode().getValue().toLowerCase().contains(searchLower)) || (product.getCategory() != null && product.getCategory()
                .toLowerCase().contains(searchLower)) || (product.getBrand() != null && product.getBrand().toLowerCase().contains(searchLower));
    }

    private ProductQueryResult toProductQueryResult(Product product) {
        return ProductQueryResult.builder().productId(product.getId()).productCode(product.getProductCode()).description(product.getDescription())
                .primaryBarcode(product.getPrimaryBarcode()).secondaryBarcodes(product.getSecondaryBarcodes()).unitOfMeasure(product.getUnitOfMeasure())
                .category(product.getCategory()).brand(product.getBrand()).createdAt(product.getCreatedAt()).lastModifiedAt(product.getLastModifiedAt()).build();
    }
}

