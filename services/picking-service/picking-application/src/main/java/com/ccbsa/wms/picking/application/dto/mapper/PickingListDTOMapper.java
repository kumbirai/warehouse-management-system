package com.ccbsa.wms.picking.application.dto.mapper;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.application.dto.command.CreatePickingListCommandDTO;
import com.ccbsa.wms.picking.application.dto.command.CreatePickingListResultDTO;
import com.ccbsa.wms.picking.application.dto.command.UploadPickingListCsvResultDTO;
import com.ccbsa.wms.picking.application.dto.query.ListPickingListsQueryResultDTO;
import com.ccbsa.wms.picking.application.dto.query.PickingListQueryResultDTO;
import com.ccbsa.wms.picking.application.service.command.dto.CreatePickingListCommand;
import com.ccbsa.wms.picking.application.service.command.dto.CreatePickingListResult;
import com.ccbsa.wms.picking.application.service.command.dto.CsvUploadResult;
import com.ccbsa.wms.picking.application.service.command.dto.UploadPickingListCsvCommand;
import com.ccbsa.wms.picking.application.service.query.dto.GetPickingListQuery;
import com.ccbsa.wms.picking.application.service.query.dto.ListPickingListsQuery;
import com.ccbsa.wms.picking.application.service.query.dto.ListPickingListsQueryResult;
import com.ccbsa.wms.picking.application.service.query.dto.PickingListQueryResult;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListStatus;

import lombok.RequiredArgsConstructor;

/**
 * DTO Mapper: PickingListDTOMapper
 * <p>
 * Maps between API DTOs and application service DTOs/commands.
 */
@Component
@RequiredArgsConstructor
public class PickingListDTOMapper {

    public UploadPickingListCsvCommand toUploadCsvCommand(MultipartFile file, String tenantId) throws IOException {
        return UploadPickingListCsvCommand.builder().tenantId(TenantId.of(tenantId)).csvContent(file.getBytes()).fileName(file.getOriginalFilename()).build();
    }

    public UploadPickingListCsvResultDTO toUploadCsvResultDTO(CsvUploadResult result) {
        List<UploadPickingListCsvResultDTO.CsvValidationErrorDTO> errorDTOs = result.getErrors().stream()
                .map(error -> UploadPickingListCsvResultDTO.CsvValidationErrorDTO.builder().rowNumber(error.getRowNumber()).fieldName(error.getFieldName())
                        .errorMessage(error.getErrorMessage()).invalidValue(error.getInvalidValue()).build()).collect(Collectors.toList());

        List<String> pickingListIds = result.getCreatedPickingListIds().stream().map(PickingListId::getValueAsString).collect(Collectors.toList());

        // Create defensive copies for builder
        return UploadPickingListCsvResultDTO.builder().totalRows(result.getTotalRows()).successfulRows(result.getSuccessfulRows()).errorRows(result.getErrorRows())
                .createdPickingListIds(new java.util.ArrayList<>(pickingListIds)).errors(new java.util.ArrayList<>(errorDTOs)).build();
    }

    public CreatePickingListCommand toCreateCommand(CreatePickingListCommandDTO dto, String tenantId) {
        List<CreatePickingListCommand.LoadCommand> loads = dto.getLoads().stream().map(loadDTO -> {
            List<CreatePickingListCommand.OrderCommand> orders = loadDTO.getOrders().stream().map(orderDTO -> {
                List<CreatePickingListCommand.OrderLineItemCommand> lineItems = orderDTO.getLineItems().stream()
                        .map(lineItemDTO -> CreatePickingListCommand.OrderLineItemCommand.builder().productCode(lineItemDTO.getProductCode()).quantity(lineItemDTO.getQuantity())
                                .notes(lineItemDTO.getNotes()).build()).collect(Collectors.toList());

                return CreatePickingListCommand.OrderCommand.builder().orderNumber(orderDTO.getOrderNumber()).customerCode(orderDTO.getCustomerCode())
                        .customerName(orderDTO.getCustomerName()).priority(orderDTO.getPriority()).lineItems(lineItems).build();
            }).collect(Collectors.toList());

            return CreatePickingListCommand.LoadCommand.builder().loadNumber(loadDTO.getLoadNumber()).orders(orders).build();
        }).collect(Collectors.toList());

        return CreatePickingListCommand.builder().tenantId(TenantId.of(tenantId)).loads(loads).notes(dto.getNotes()).build();
    }

    public CreatePickingListResultDTO toCreateResultDTO(CreatePickingListResult result) {
        String statusValue = result.getStatus() != null ? result.getStatus().name() : null;
        return CreatePickingListResultDTO.builder().pickingListId(result.getPickingListId().getValueAsString()).status(statusValue).receivedAt(result.getReceivedAt())
                .loadCount(result.getLoadCount()).build();
    }

    public GetPickingListQuery toGetQuery(String id, String tenantId) {
        return GetPickingListQuery.builder().tenantId(TenantId.of(tenantId)).pickingListId(PickingListId.of(id)).build();
    }

    public ListPickingListsQuery toListQuery(String tenantId, String status, int page, int size) {
        PickingListStatus statusEnum = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = PickingListStatus.valueOf(status.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        String.format("Invalid picking list status: '%s'. Valid values are: %s", status, java.util.Arrays.toString(PickingListStatus.values())));
            }
        }
        return ListPickingListsQuery.builder().tenantId(TenantId.of(tenantId)).status(statusEnum).page(page).size(size).build();
    }

    public PickingListQueryResultDTO toQueryResultDTO(PickingListQueryResult result) {
        List<PickingListQueryResultDTO.LoadQueryResultDTO> loadDTOs = result.getLoads() != null ? result.getLoads().stream().map(loadResult -> {
            List<PickingListQueryResultDTO.OrderQueryResultDTO> orderDTOs = loadResult.getOrders() != null ? loadResult.getOrders().stream().map(orderResult -> {
                List<PickingListQueryResultDTO.OrderLineItemQueryResultDTO> lineItemDTOs = orderResult.getLineItems() != null ? orderResult.getLineItems().stream()
                        .map(lineItemResult -> PickingListQueryResultDTO.OrderLineItemQueryResultDTO.builder().lineItemId(lineItemResult.getLineItemId())
                                .productCode(lineItemResult.getProductCode()).productDescription(lineItemResult.getProductDescription()).quantity(lineItemResult.getQuantity())
                                .notes(lineItemResult.getNotes()).build()).collect(Collectors.toList()) : List.of();

                // Create defensive copy of line items list for builder
                return PickingListQueryResultDTO.OrderQueryResultDTO.builder().orderId(orderResult.getOrderId()).orderNumber(orderResult.getOrderNumber())
                        .customerCode(orderResult.getCustomerCode()).customerName(orderResult.getCustomerName()).priority(orderResult.getPriority()).status(orderResult.getStatus())
                        .lineItems(new java.util.ArrayList<>(lineItemDTOs)).build();
            }).collect(Collectors.toList()) : List.of();

            // Create defensive copy of orders list for builder
            return PickingListQueryResultDTO.LoadQueryResultDTO.builder().loadId(loadResult.getLoadId()).loadNumber(loadResult.getLoadNumber()).status(loadResult.getStatus())
                    .orderCount(loadResult.getOrderCount()).orders(new java.util.ArrayList<>(orderDTOs)).build();
        }).collect(Collectors.toList()) : List.of();

        String statusValue = result.getStatus() != null ? result.getStatus().name() : null;
        // Create defensive copy of loads list for builder
        return PickingListQueryResultDTO.builder().id(result.getId().getValueAsString())
                .pickingListReference(result.getPickingListReference() != null ? result.getPickingListReference().getValue() : null).status(statusValue)
                .receivedAt(result.getReceivedAt()).processedAt(result.getProcessedAt()).loadCount(result.getLoadCount()).totalOrderCount(result.getTotalOrderCount())
                .notes(result.getNotes()).loads(new java.util.ArrayList<>(loadDTOs)).build();
    }

    public ListPickingListsQueryResultDTO toListQueryResultDTO(ListPickingListsQueryResult result) {
        List<ListPickingListsQueryResultDTO.PickingListViewDTO> viewDTOs = result.getPickingLists().stream().map(view -> {
            String statusValue = view.getStatus() != null ? view.getStatus().name() : null;
            return ListPickingListsQueryResultDTO.PickingListViewDTO.builder().id(view.getId().getValueAsString())
                    .pickingListReference(view.getPickingListReference() != null ? view.getPickingListReference().getValue() : null).status(statusValue)
                    .loadCount(view.getLoadCount()).totalOrderCount(view.getTotalOrderCount()).build();
        }).collect(Collectors.toList());

        // Create defensive copy of picking lists for builder
        return ListPickingListsQueryResultDTO.builder().pickingLists(new java.util.ArrayList<>(viewDTOs)).totalElements(result.getTotalElements()).page(result.getPage())
                .size(result.getSize()).totalPages(result.getTotalPages()).build();
    }
}
