package com.ccbsa.wms.stock.application.service.query.dto;

import java.util.List;

/**
 * Query Result DTO: ListConsignmentsQueryResult
 * <p>
 * Result object returned from list consignments query. Contains a list of consignment query results with pagination metadata.
 */
public final class ListConsignmentsQueryResult {
    private final List<ConsignmentQueryResult> consignments;
    private final Integer totalCount;
    private final Integer page;
    private final Integer size;

    private ListConsignmentsQueryResult(Builder builder) {
        this.consignments = builder.consignments != null ? List.copyOf(builder.consignments) : List.of();
        this.totalCount = builder.totalCount;
        this.page = builder.page;
        this.size = builder.size;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<ConsignmentQueryResult> getConsignments() {
        return consignments;
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
        private List<ConsignmentQueryResult> consignments;
        private Integer totalCount;
        private Integer page;
        private Integer size;

        public Builder consignments(List<ConsignmentQueryResult> consignments) {
            this.consignments = consignments != null ? List.copyOf(consignments) : null;
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

        public ListConsignmentsQueryResult build() {
            return new ListConsignmentsQueryResult(this);
        }
    }
}

