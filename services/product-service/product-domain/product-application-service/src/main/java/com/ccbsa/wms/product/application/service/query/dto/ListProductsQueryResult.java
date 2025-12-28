package com.ccbsa.wms.product.application.service.query.dto;

import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: ListProductsQueryResult
 * <p>
 * Result object returned from list products query. Contains a list of product query results.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Defensive copy made in constructor and getter returns immutable view.")
public final class ListProductsQueryResult {
    private final List<ProductQueryResult> products;
    private final Integer totalCount;
    private final Integer page;
    private final Integer size;

    public ListProductsQueryResult(List<ProductQueryResult> products, Integer totalCount, Integer page, Integer size) {
        // Defensive copy to prevent external modification
        this.products = products != null ? List.copyOf(products) : List.of();
        this.totalCount = totalCount;
        this.page = page;
        this.size = size;
    }

    /**
     * Returns an unmodifiable view of the products list.
     *
     * @return Unmodifiable list of products
     */
    public List<ProductQueryResult> getProducts() {
        return Collections.unmodifiableList(products);
    }
}

