package com.ccbsa.wms.stock.application.service.query.dto;

import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: ListConsignmentsQueryResult
 * <p>
 * Result object returned from list consignments query. Contains a list of consignment query results with pagination metadata.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Defensive copy made in constructor and getter returns immutable view.")
public final class ListConsignmentsQueryResult {
    private final List<ConsignmentQueryResult> consignments;
    private final Integer totalCount;
    private final Integer page;
    private final Integer size;

    public ListConsignmentsQueryResult(List<ConsignmentQueryResult> consignments, Integer totalCount, Integer page, Integer size) {
        // Defensive copy to prevent external modification
        this.consignments = consignments != null ? List.copyOf(consignments) : List.of();
        this.totalCount = totalCount;
        this.page = page;
        this.size = size;
    }

    /**
     * Returns an unmodifiable view of the consignments list.
     *
     * @return Unmodifiable list of consignments
     */
    public List<ConsignmentQueryResult> getConsignments() {
        return Collections.unmodifiableList(consignments);
    }
}

