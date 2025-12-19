package com.ccbsa.wms.product.application.dto.query;

import java.util.List;

/**
 * Query Result DTO: ListProductsQueryResultDTO
 * <p>
 * API response DTO for list products query results.
 */
public final class ListProductsQueryResultDTO {
    private List<ProductQueryResultDTO> products;
    private Integer totalCount;
    private Integer page;
    private Integer size;

    public List<ProductQueryResultDTO> getProducts() {
        return products;
    }

    public void setProducts(List<ProductQueryResultDTO> products) {
        this.products = products;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}

