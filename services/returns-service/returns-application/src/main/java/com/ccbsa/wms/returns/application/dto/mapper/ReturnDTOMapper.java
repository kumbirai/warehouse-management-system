package com.ccbsa.wms.returns.application.dto.mapper;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.Notes;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.ProductCondition;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.ReturnReason;
import com.ccbsa.wms.returns.application.dto.command.HandlePartialOrderAcceptanceRequestDTO;
import com.ccbsa.wms.returns.application.dto.command.HandlePartialOrderAcceptanceResponseDTO;
import com.ccbsa.wms.returns.application.dto.command.ProcessFullOrderReturnRequestDTO;
import com.ccbsa.wms.returns.application.dto.command.ProcessFullOrderReturnResponseDTO;
import com.ccbsa.wms.returns.application.dto.command.RecordDamageAssessmentRequestDTO;
import com.ccbsa.wms.returns.application.dto.command.RecordDamageAssessmentResponseDTO;
import com.ccbsa.wms.returns.application.dto.query.ReturnQueryDTO;
import com.ccbsa.wms.returns.application.service.command.dto.HandlePartialOrderAcceptanceCommand;
import com.ccbsa.wms.returns.application.service.command.dto.HandlePartialOrderAcceptanceResult;
import com.ccbsa.wms.returns.application.service.command.dto.ProcessFullOrderReturnCommand;
import com.ccbsa.wms.returns.application.service.command.dto.ProcessFullOrderReturnResult;
import com.ccbsa.wms.returns.application.service.command.dto.RecordDamageAssessmentCommand;
import com.ccbsa.wms.returns.application.service.command.dto.RecordDamageAssessmentResult;
import com.ccbsa.wms.returns.application.service.query.dto.GetReturnQueryResult;
import com.ccbsa.wms.returns.domain.core.valueobject.CustomerSignature;
import com.ccbsa.wms.returns.domain.core.valueobject.DamageAssessmentId;
import com.ccbsa.wms.returns.domain.core.valueobject.DamagedProductItemId;
import com.ccbsa.wms.returns.domain.core.valueobject.InsuranceClaimInfo;
import com.ccbsa.common.domain.valueobject.ReturnId;
import com.ccbsa.common.domain.valueobject.ReturnLineItemId;

import lombok.extern.slf4j.Slf4j;

/**
 * DTO Mapper: ReturnDTOMapper
 * <p>
 * Maps between API DTOs and application service commands/queries. Acts as an anti-corruption layer protecting the domain from external API changes.
 */
@Component
@Slf4j
public class ReturnDTOMapper {

    /**
     * Converts HandlePartialOrderAcceptanceRequestDTO to HandlePartialOrderAcceptanceCommand.
     *
     * @param dto Request DTO
     * @return HandlePartialOrderAcceptanceCommand
     */
    public HandlePartialOrderAcceptanceCommand toHandlePartialOrderAcceptanceCommand(HandlePartialOrderAcceptanceRequestDTO dto) {
        List<HandlePartialOrderAcceptanceCommand.PartialReturnLineItemCommand> lineItemCommands = dto.getLineItems().stream()
                .map(item -> HandlePartialOrderAcceptanceCommand.PartialReturnLineItemCommand.builder().lineItemId(ReturnLineItemId.generate())
                        .productId(ProductId.of(item.getProductId())).orderedQuantity(Quantity.of(item.getOrderedQuantity())).pickedQuantity(Quantity.of(item.getPickedQuantity()))
                        .acceptedQuantity(Quantity.of(item.getAcceptedQuantity()))
                        .returnReason(item.getReturnReason() != null ? ReturnReason.valueOf(item.getReturnReason()) : null)
                        .lineNotes(item.getLineNotes() != null ? Notes.forLineItem(item.getLineNotes()) : Notes.forLineItem(null)).build()).collect(Collectors.toList());

        return HandlePartialOrderAcceptanceCommand.builder().orderNumber(OrderNumber.of(dto.getOrderNumber())).lineItems(lineItemCommands).signatureData(dto.getSignatureData())
                .signedAt(dto.getSignedAt()).build();
    }

    /**
     * Converts HandlePartialOrderAcceptanceResult to HandlePartialOrderAcceptanceResponseDTO.
     *
     * @param result Command result
     * @return HandlePartialOrderAcceptanceResponseDTO
     */
    public HandlePartialOrderAcceptanceResponseDTO toHandlePartialOrderAcceptanceResponseDTO(HandlePartialOrderAcceptanceResult result) {
        return HandlePartialOrderAcceptanceResponseDTO.builder().returnId(result.getReturnId().getValueAsString()).orderNumber(result.getOrderNumber().getValue())
                .returnType(result.getReturnType().name()).status(result.getStatus().name()).returnedAt(result.getReturnedAt()).build();
    }

    /**
     * Converts ProcessFullOrderReturnRequestDTO to ProcessFullOrderReturnCommand.
     *
     * @param dto Request DTO
     * @return ProcessFullOrderReturnCommand
     */
    public ProcessFullOrderReturnCommand toProcessFullOrderReturnCommand(ProcessFullOrderReturnRequestDTO dto) {
        List<ProcessFullOrderReturnCommand.FullReturnLineItemCommand> lineItemCommands = dto.getLineItems().stream()
                .map(item -> ProcessFullOrderReturnCommand.FullReturnLineItemCommand.builder().lineItemId(ReturnLineItemId.generate()).productId(ProductId.of(item.getProductId()))
                        .orderedQuantity(Quantity.of(item.getOrderedQuantity())).pickedQuantity(Quantity.of(item.getPickedQuantity()))
                        .productCondition(ProductCondition.valueOf(item.getProductCondition())).returnReason(ReturnReason.valueOf(item.getReturnReason()))
                        .lineNotes(item.getLineNotes() != null ? Notes.forLineItem(item.getLineNotes()) : Notes.forLineItem(null)).build()).collect(Collectors.toList());

        Notes returnNotes = dto.getReturnNotes() != null ? Notes.of(dto.getReturnNotes()) : Notes.of(null);
        return ProcessFullOrderReturnCommand.builder().orderNumber(OrderNumber.of(dto.getOrderNumber())).lineItems(lineItemCommands)
                .primaryReturnReason(ReturnReason.valueOf(dto.getPrimaryReturnReason())).returnNotes(returnNotes).build();
    }

    /**
     * Converts ProcessFullOrderReturnResult to ProcessFullOrderReturnResponseDTO.
     *
     * @param result Command result
     * @return ProcessFullOrderReturnResponseDTO
     */
    public ProcessFullOrderReturnResponseDTO toProcessFullOrderReturnResponseDTO(ProcessFullOrderReturnResult result) {
        return ProcessFullOrderReturnResponseDTO.builder().returnId(result.getReturnId().getValueAsString()).orderNumber(result.getOrderNumber().getValue())
                .returnType(result.getReturnType().name()).status(result.getStatus().name()).primaryReturnReason(result.getPrimaryReturnReason().name())
                .returnedAt(result.getReturnedAt()).build();
    }

    /**
     * Converts RecordDamageAssessmentRequestDTO to RecordDamageAssessmentCommand.
     *
     * @param dto Request DTO
     * @return RecordDamageAssessmentCommand
     */
    public RecordDamageAssessmentCommand toRecordDamageAssessmentCommand(RecordDamageAssessmentRequestDTO dto) {
        List<RecordDamageAssessmentCommand.DamagedProductCommand> damagedProductCommands = dto.getDamagedProducts().stream()
                .map(item -> RecordDamageAssessmentCommand.DamagedProductCommand.builder().itemId(DamagedProductItemId.generate()).productId(ProductId.of(item.getProductId()))
                        .damagedQuantity(Quantity.of(item.getDamagedQuantity())).damageType(dto.getDamageType()).damageSeverity(dto.getDamageSeverity())
                        .damageSource(dto.getDamageSource()).photoUrl(item.getPhotoUrl())
                        .notes(item.getNotes() != null ? Notes.forLineItem(item.getNotes()) : Notes.forLineItem(null)).build()).collect(Collectors.toList());

        InsuranceClaimInfo insuranceClaimInfo = null;
        if (dto.getInsuranceClaim() != null) {
            insuranceClaimInfo =
                    InsuranceClaimInfo.of(dto.getInsuranceClaim().getClaimNumber(), dto.getInsuranceClaim().getInsuranceCompany(), dto.getInsuranceClaim().getClaimStatus(),
                            dto.getInsuranceClaim().getClaimAmount());
        }

        Notes damageNotes = dto.getDamageNotes() != null ? Notes.of(dto.getDamageNotes()) : Notes.of(null);
        return RecordDamageAssessmentCommand.builder().orderNumber(OrderNumber.of(dto.getOrderNumber())).damageType(dto.getDamageType()).damageSeverity(dto.getDamageSeverity())
                .damageSource(dto.getDamageSource()).damagedProducts(damagedProductCommands).insuranceClaimInfo(insuranceClaimInfo).damageNotes(damageNotes).build();
    }

    /**
     * Converts RecordDamageAssessmentResult to RecordDamageAssessmentResponseDTO.
     *
     * @param result Command result
     * @return RecordDamageAssessmentResponseDTO
     */
    public RecordDamageAssessmentResponseDTO toRecordDamageAssessmentResponseDTO(RecordDamageAssessmentResult result) {
        return RecordDamageAssessmentResponseDTO.builder().damageAssessmentId(result.getDamageAssessmentId().getValueAsString()).orderNumber(result.getOrderNumber().getValue())
                .damageType(result.getDamageType()).damageSeverity(result.getDamageSeverity()).damageSource(result.getDamageSource()).status(result.getStatus().name())
                .recordedAt(result.getRecordedAt()).build();
    }

    /**
     * Converts GetReturnQueryResult to ReturnQueryDTO.
     *
     * @param result Query result
     * @return ReturnQueryDTO
     */
    public ReturnQueryDTO toReturnQueryDTO(GetReturnQueryResult result) {
        List<ReturnQueryDTO.ReturnLineItemQueryDTO> lineItemDTOs = result.getLineItems().stream()
                .map(item -> ReturnQueryDTO.ReturnLineItemQueryDTO.builder()
                        .lineItemId(item.getLineItemId())
                        .productId(item.getProductId())
                        .orderedQuantity(item.getOrderedQuantity())
                        .pickedQuantity(item.getPickedQuantity())
                        .acceptedQuantity(item.getAcceptedQuantity())
                        .returnedQuantity(item.getReturnedQuantity())
                        .productCondition(item.getProductCondition())
                        .returnReason(item.getReturnReason() != null ? item.getReturnReason().name() : null)
                        .lineNotes(item.getLineNotes())
                        .build())
                .collect(Collectors.toList());

        return ReturnQueryDTO.builder()
                .returnId(result.getReturnId().getValueAsString())
                .orderNumber(result.getOrderNumber().getValue())
                .returnType(result.getReturnType().name())
                .status(result.getStatus().name())
                .lineItems(lineItemDTOs)
                .primaryReturnReason(result.getPrimaryReturnReason() != null ? result.getPrimaryReturnReason().name() : null)
                .returnNotes(result.getReturnNotes())
                .returnedAt(result.getReturnedAt())
                .createdAt(result.getCreatedAt())
                .lastModifiedAt(result.getLastModifiedAt())
                .build();
    }
}
