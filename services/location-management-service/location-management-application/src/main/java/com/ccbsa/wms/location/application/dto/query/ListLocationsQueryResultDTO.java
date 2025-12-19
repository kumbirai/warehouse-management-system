package com.ccbsa.wms.location.application.dto.query;

import java.util.List;

/**
 * Query Result DTO: ListLocationsQueryResultDTO
 * <p>
 * API response DTO for list locations query results.
 */
public final class ListLocationsQueryResultDTO {
    private List<LocationQueryResultDTO> locations;
    private Integer totalCount;
    private Integer page;
    private Integer size;

    public List<LocationQueryResultDTO> getLocations() {
        return locations;
    }

    public void setLocations(List<LocationQueryResultDTO> locations) {
        this.locations = locations;
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

