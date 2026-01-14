package com.ccbsa.wms.returns.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.valueobject.ReturnStatus;
import com.ccbsa.wms.returns.application.service.port.repository.ReturnRepository;
import com.ccbsa.wms.returns.application.service.query.dto.GetReturnQuery;
import com.ccbsa.wms.returns.application.service.query.dto.GetReturnQueryResult;
import com.ccbsa.wms.returns.domain.core.entity.Return;
import com.ccbsa.wms.returns.domain.core.entity.ReturnLineItem;
import com.ccbsa.wms.returns.domain.core.exception.ReturnNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: GetReturnQueryHandler
 * <p>
 * Handles retrieval of Return aggregate by ID.
 * <p>
 * Responsibilities:
 * - Load Return aggregate from repository
 * - Map aggregate to query result DTO
 * - Return optimized read model
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GetReturnQueryHandler {
    private final ReturnRepository returnRepository;

    @Transactional(readOnly = true)
    public GetReturnQueryResult handle(GetReturnQuery query) {
        log.debug("Handling GetReturnQuery for returnId: {}, tenant: {}", query.getReturnId().getValueAsString(), query.getTenantId().getValue());

        // Load aggregate from repository
        Return returnAggregate = returnRepository.findByIdAndTenantId(query.getReturnId(), query.getTenantId())
                .orElseThrow(() -> new ReturnNotFoundException(String.format("Return not found: %s", query.getReturnId().getValueAsString())));

        // Map to query result
        return mapToQueryResult(returnAggregate);
    }

    GetReturnQueryResult mapToQueryResult(Return returnAggregate) {
        List<GetReturnQueryResult.ReturnLineItemResult> lineItemResults = returnAggregate.getLineItems().stream()
                .map(this::mapLineItem)
                .collect(Collectors.toList());

        return GetReturnQueryResult.builder()
                .returnId(returnAggregate.getId())
                .orderNumber(returnAggregate.getOrderNumber())
                .tenantId(returnAggregate.getTenantId())
                .returnType(returnAggregate.getReturnType())
                .status(returnAggregate.getStatus())
                .lineItems(lineItemResults)
                .primaryReturnReason(returnAggregate.getPrimaryReturnReason())
                .returnNotes(returnAggregate.getReturnNotes() != null ? returnAggregate.getReturnNotes().getValue() : null)
                .returnedAt(returnAggregate.getReturnedAt())
                .createdAt(returnAggregate.getCreatedAt())
                .lastModifiedAt(returnAggregate.getLastModifiedAt())
                .build();
    }

    private GetReturnQueryResult.ReturnLineItemResult mapLineItem(ReturnLineItem lineItem) {
        return GetReturnQueryResult.ReturnLineItemResult.builder()
                .lineItemId(lineItem.getId().getValueAsString())
                .productId(lineItem.getProductId().getValueAsString())
                .orderedQuantity(lineItem.getOrderedQuantity().getValue())
                .pickedQuantity(lineItem.getPickedQuantity().getValue())
                .acceptedQuantity(lineItem.getAcceptedQuantity().getValue())
                .returnedQuantity(lineItem.getReturnedQuantity().getValue())
                .productCondition(lineItem.getProductCondition() != null ? lineItem.getProductCondition().name() : null)
                .returnReason(lineItem.getReturnReason())
                .lineNotes(lineItem.getLineNotes() != null ? lineItem.getLineNotes().getValue() : null)
                .build();
    }
}
