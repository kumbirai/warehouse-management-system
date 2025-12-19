package com.ccbsa.wms.product.application.service.query.dto;

import java.util.List;

/**
 * Query Result DTO: ListProductsQueryResult
 * <p>
 * Result object returned from list products query. Contains a list of product query results.
 */
public final class ListProductsQueryResult {
    private final List<ProductQueryResult> products;
    private final Integer totalCount;
    private final Integer page;
    private final Integer size;

    private ListProductsQueryResult(Builder builder) {
        this.products = builder.products != null ? List.copyOf(builder.products) : List.of();
        this.totalCount = builder.totalCount;
        this.page = builder.page;
        this.size = builder.size;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<ProductQueryResult> getProducts() {
        return products;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getSize() {
        return size;
    }

    public static class Builder {
        private List<ProductQueryResult> products;
        private Integer totalCount;
        private Integer page;
        private Integer size;

        public Builder products(List<ProductQueryResult> products) {
            this.products = products;
            return this;
        }

        public Builder totalCount(Integer totalCount) {
            this.totalCount = totalCount;
            return this;
        }

        public Builder page(Integer page) {
            this.page = page;
            return this;
        }

        public Builder size(Integer size) {
            this.size = size;
            return this;
        }

        public ListProductsQueryResult build() {
            return new ListProductsQueryResult(this);
        }
    }
}

