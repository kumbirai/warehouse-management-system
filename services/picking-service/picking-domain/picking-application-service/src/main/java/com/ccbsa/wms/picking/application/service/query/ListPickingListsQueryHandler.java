package com.ccbsa.wms.picking.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.picking.application.service.port.data.PickingListViewRepository;
import com.ccbsa.wms.picking.application.service.query.dto.ListPickingListsQuery;
import com.ccbsa.wms.picking.application.service.query.dto.ListPickingListsQueryResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: ListPickingListsQueryHandler
 * <p>
 * Handles query for listing picking lists with filtering and pagination.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ListPickingListsQueryHandler {
    private final PickingListViewRepository viewRepository;

    @Transactional(readOnly = true)
    public ListPickingListsQueryResult handle(ListPickingListsQuery query) {
        var pickingLists = viewRepository.findByTenantId(query.getTenantId(), query.getStatus(), query.getPage(), query.getSize());

        long totalElements = viewRepository.countByTenantId(query.getTenantId(), query.getStatus());

        int totalPages = (int) Math.ceil((double) totalElements / query.getSize());

        return ListPickingListsQueryResult.builder().pickingLists(pickingLists).totalElements((int) totalElements).page(query.getPage()).size(query.getSize())
                .totalPages(totalPages).build();
    }
}
