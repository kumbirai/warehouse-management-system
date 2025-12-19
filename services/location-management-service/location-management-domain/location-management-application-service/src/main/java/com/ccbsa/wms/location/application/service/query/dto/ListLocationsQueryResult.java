package com.ccbsa.wms.location.application.service.query.dto;

import java.util.List;

/**
 * Query Result DTO: ListLocationsQueryResult
 * <p>
 * Result object returned from list locations query. Contains a list of location query results.
 */
public final class ListLocationsQueryResult {
    private final List<LocationQueryResult> locations;
    private final Integer totalCount;
    private final Integer page;
    private final Integer size;

    private ListLocationsQueryResult(Builder builder) {
        this.locations = builder.locations != null ? List.copyOf(builder.locations) : List.of();
        this.totalCount = builder.totalCount;
        this.page = builder.page;
        this.size = builder.size;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<LocationQueryResult> getLocations() {
        return locations;
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
        private List<LocationQueryResult> locations;
        private Integer totalCount;
        private Integer page;
        private Integer size;

        public Builder locations(List<LocationQueryResult> locations) {
            this.locations = locations;
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

        public ListLocationsQueryResult build() {
            return new ListLocationsQueryResult(this);
        }
    }
}

