package com.ccbsa.wms.picking.application.dto.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.application.dto.command.CreatePickingTaskCommandDTO;
import com.ccbsa.wms.picking.application.dto.command.CreatePickingTaskResultDTO;
import com.ccbsa.wms.picking.application.dto.query.ListPickingTasksQueryResultDTO;
import com.ccbsa.wms.picking.application.service.command.dto.CreatePickingTaskCommand;
import com.ccbsa.wms.picking.application.service.command.dto.CreatePickingTaskResult;
import com.ccbsa.wms.picking.application.service.query.dto.ListPickingTasksQuery;
import com.ccbsa.wms.picking.application.service.query.dto.ListPickingTasksQueryResult;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskStatus;

import lombok.RequiredArgsConstructor;

/**
 * DTO Mapper: PickingTaskDTOMapper
 * <p>
 * Maps between API DTOs and application service query/command DTOs.
 */
@Component
@RequiredArgsConstructor
public class PickingTaskDTOMapper {

    public ListPickingTasksQuery toListQuery(String tenantId, String status, int page, int size) {
        PickingTaskStatus statusEnum = status != null ? PickingTaskStatus.valueOf(status) : null;
        return ListPickingTasksQuery.builder().tenantId(TenantId.of(tenantId)).status(statusEnum).page(page).size(size).build();
    }

    public ListPickingTasksQueryResultDTO toListQueryResultDTO(ListPickingTasksQueryResult result) {
        List<ListPickingTasksQueryResultDTO.PickingTaskQueryResultDTO> taskDTOs = result.getPickingTasks().stream()
                .map(task -> ListPickingTasksQueryResultDTO.PickingTaskQueryResultDTO.builder().taskId(task.getId().getValueAsString()).loadId(task.getLoadId().getValueAsString())
                        .orderId(task.getOrderId().getValueAsString()).productCode(task.getProductCode().getValue()).locationId(task.getLocationId().getValueAsString())
                        .quantity(task.getQuantity().getValue()).status(task.getStatus().name()).sequence(task.getSequence()).build()).collect(Collectors.toList());

        return ListPickingTasksQueryResultDTO.builder().pickingTasks(taskDTOs).totalElements(result.getTotalElements()).page(result.getPage()).size(result.getSize())
                .totalPages(result.getTotalPages()).build();
    }

    public CreatePickingTaskCommand toCreateCommand(CreatePickingTaskCommandDTO commandDTO, String tenantId) {
        List<CreatePickingTaskCommand.PickingItemCommand> items = commandDTO.getItems().stream()
                .map(item -> CreatePickingTaskCommand.PickingItemCommand.builder().productId(item.getProductId()).quantity(item.getQuantity()).locationId(item.getLocationId())
                        .build()).collect(Collectors.toList());

        return CreatePickingTaskCommand.builder().tenantId(TenantId.of(tenantId)).orderId(commandDTO.getOrderId()).items(items).priority(commandDTO.getPriority())
                .dueDate(commandDTO.getDueDate()).build();
    }

    public CreatePickingTaskResultDTO toCreateResultDTO(CreatePickingTaskResult result) {
        return CreatePickingTaskResultDTO.builder().taskId(result.getTaskId().getValueAsString()).status(result.getStatus()).orderId(result.getOrderId()).build();
    }
}
