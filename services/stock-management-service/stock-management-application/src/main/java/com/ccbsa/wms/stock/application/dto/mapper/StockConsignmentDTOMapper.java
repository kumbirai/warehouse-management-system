package com.ccbsa.wms.stock.application.dto.mapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.WarehouseId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.stock.application.dto.command.CreateConsignmentCommandDTO;
import com.ccbsa.wms.stock.application.dto.command.CreateConsignmentResultDTO;
import com.ccbsa.wms.stock.application.dto.command.UploadConsignmentCsvResultDTO;
import com.ccbsa.wms.stock.application.dto.command.ValidateConsignmentCommandDTO;
import com.ccbsa.wms.stock.application.dto.command.ValidateConsignmentResultDTO;
import com.ccbsa.wms.stock.application.dto.query.ConsignmentQueryDTO;
import com.ccbsa.wms.stock.application.service.command.dto.ConsignmentCsvError;
import com.ccbsa.wms.stock.application.service.command.dto.CreateConsignmentCommand;
import com.ccbsa.wms.stock.application.service.command.dto.CreateConsignmentResult;
import com.ccbsa.wms.stock.application.service.command.dto.UploadConsignmentCsvCommand;
import com.ccbsa.wms.stock.application.service.command.dto.UploadConsignmentCsvResult;
import com.ccbsa.wms.stock.application.service.command.dto.ValidateConsignmentCommand;
import com.ccbsa.wms.stock.application.service.command.dto.ValidateConsignmentResult;
import com.ccbsa.wms.stock.application.service.query.dto.ConsignmentQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.GetConsignmentQuery;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentLineItem;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentReference;
import com.ccbsa.wms.stock.domain.core.valueobject.ReceivedBy;

/**
 * DTO Mapper: StockConsignmentDTOMapper
 * <p>
 * Maps between API DTOs and application service commands/queries. Acts as an anti-corruption layer protecting the domain from external API changes.
 */
@Component
public class StockConsignmentDTOMapper {

    /**
     * Converts CreateConsignmentCommandDTO to CreateConsignmentCommand.
     *
     * @param dto      Command DTO
     * @param tenantId Tenant identifier string
     * @return CreateConsignmentCommand
     */
    public CreateConsignmentCommand toCreateCommand(CreateConsignmentCommandDTO dto, String tenantId) {
        List<ConsignmentLineItem> lineItems = dto.getLineItems().stream()
                .map(item -> ConsignmentLineItem.builder().productCode(ProductCode.of(item.getProductCode())).quantity(item.getQuantity()).expirationDate(item.getExpirationDate())
                        .build()).collect(Collectors.toList());

        return CreateConsignmentCommand.builder().tenantId(TenantId.of(tenantId)).consignmentReference(ConsignmentReference.of(dto.getConsignmentReference()))
                .warehouseId(WarehouseId.of(dto.getWarehouseId())).receivedAt(dto.getReceivedAt()).receivedBy(ReceivedBy.of(dto.getReceivedBy())).lineItems(lineItems).build();
    }

    /**
     * Converts CreateConsignmentResult to CreateConsignmentResultDTO.
     *
     * @param result Command result
     * @return CreateConsignmentResultDTO
     */
    public CreateConsignmentResultDTO toCreateResultDTO(CreateConsignmentResult result) {
        CreateConsignmentResultDTO dto = new CreateConsignmentResultDTO();
        dto.setConsignmentId(result.getConsignmentId().getValueAsString());
        dto.setStatus(result.getStatus().name());
        dto.setReceivedAt(result.getReceivedAt());
        return dto;
    }

    /**
     * Converts MultipartFile to UploadConsignmentCsvCommand.
     *
     * @param file       CSV file
     * @param tenantId   Tenant identifier string
     * @param receivedBy User who received the consignment
     * @return UploadConsignmentCsvCommand
     * @throws IOException if reading file fails
     */
    public UploadConsignmentCsvCommand toUploadCsvCommand(MultipartFile file, String tenantId, String receivedBy) throws IOException {
        InputStream inputStream = file.getInputStream();
        return UploadConsignmentCsvCommand.builder().tenantId(TenantId.of(tenantId)).csvInputStream(inputStream).receivedBy(ReceivedBy.of(receivedBy)).build();
    }

    /**
     * Converts UploadConsignmentCsvResult to UploadConsignmentCsvResultDTO.
     *
     * @param result Command result
     * @return UploadConsignmentCsvResultDTO
     */
    public UploadConsignmentCsvResultDTO toUploadCsvResultDTO(UploadConsignmentCsvResult result) {
        UploadConsignmentCsvResultDTO dto = new UploadConsignmentCsvResultDTO();
        dto.setTotalRows(result.getTotalRows());
        dto.setProcessedRows(result.getProcessedRows());
        dto.setCreatedConsignments(result.getCreatedConsignments());
        dto.setErrorRows(result.getErrorRows());

        List<UploadConsignmentCsvResultDTO.ConsignmentCsvErrorDTO> errorDTOs = result.getErrors().stream().map(this::toCsvErrorDTO).collect(Collectors.toList());
        dto.setErrors(errorDTOs);

        return dto;
    }

    /**
     * Converts ConsignmentCsvError to ConsignmentCsvErrorDTO.
     *
     * @param error Error object
     * @return ConsignmentCsvErrorDTO
     */
    private UploadConsignmentCsvResultDTO.ConsignmentCsvErrorDTO toCsvErrorDTO(ConsignmentCsvError error) {
        UploadConsignmentCsvResultDTO.ConsignmentCsvErrorDTO dto = new UploadConsignmentCsvResultDTO.ConsignmentCsvErrorDTO();
        dto.setRowNumber(error.getRowNumber());
        dto.setConsignmentReference(error.getConsignmentReference());
        dto.setProductCode(error.getProductCode());
        dto.setErrorMessage(error.getErrorMessage());
        return dto;
    }

    /**
     * Converts ValidateConsignmentCommandDTO to ValidateConsignmentCommand.
     *
     * @param dto      Command DTO
     * @param tenantId Tenant identifier string
     * @return ValidateConsignmentCommand
     */
    public ValidateConsignmentCommand toValidateCommand(ValidateConsignmentCommandDTO dto, String tenantId) {
        List<ConsignmentLineItem> lineItems = dto.getLineItems().stream()
                .map(item -> ConsignmentLineItem.builder().productCode(ProductCode.of(item.getProductCode())).quantity(item.getQuantity()).expirationDate(item.getExpirationDate())
                        .build()).collect(Collectors.toList());

        return ValidateConsignmentCommand.builder().tenantId(TenantId.of(tenantId)).consignmentReference(ConsignmentReference.of(dto.getConsignmentReference()))
                .warehouseId(WarehouseId.of(dto.getWarehouseId())).receivedAt(dto.getReceivedAt()).lineItems(lineItems).build();
    }

    /**
     * Converts ValidateConsignmentResult to ValidateConsignmentResultDTO.
     *
     * @param result Command result
     * @return ValidateConsignmentResultDTO
     */
    public ValidateConsignmentResultDTO toValidateResultDTO(ValidateConsignmentResult result) {
        ValidateConsignmentResultDTO dto = new ValidateConsignmentResultDTO();
        dto.setValid(result.isValid());
        dto.setValidationErrors(result.getValidationErrors());
        return dto;
    }

    /**
     * Converts consignment ID string and tenant ID to GetConsignmentQuery.
     *
     * @param consignmentId Consignment ID string
     * @param tenantId      Tenant ID string
     * @return GetConsignmentQuery
     */
    public GetConsignmentQuery toGetConsignmentQuery(String consignmentId, String tenantId) {
        return GetConsignmentQuery.builder().consignmentId(ConsignmentId.of(consignmentId)).tenantId(TenantId.of(tenantId)).build();
    }

    /**
     * Converts ConsignmentQueryResult to ConsignmentQueryDTO.
     *
     * @param result Query result
     * @return ConsignmentQueryDTO
     */
    public ConsignmentQueryDTO toQueryResultDTO(ConsignmentQueryResult result) {
        ConsignmentQueryDTO dto = new ConsignmentQueryDTO();
        dto.setConsignmentId(result.getConsignmentId().getValueAsString());
        dto.setConsignmentReference(result.getConsignmentReference().getValue());
        dto.setWarehouseId(result.getWarehouseId().getValue());
        dto.setStatus(result.getStatus().name());
        dto.setReceivedAt(result.getReceivedAt());
        dto.setConfirmedAt(result.getConfirmedAt());
        dto.setReceivedBy(result.getReceivedBy() != null ? result.getReceivedBy().getValue() : null);
        dto.setCreatedAt(result.getCreatedAt());
        dto.setLastModifiedAt(result.getLastModifiedAt());

        // Map line items
        List<ConsignmentQueryDTO.ConsignmentLineItemDTO> lineItemDTOs = result.getLineItems().stream().map(item -> {
            ConsignmentQueryDTO.ConsignmentLineItemDTO itemDTO = new ConsignmentQueryDTO.ConsignmentLineItemDTO();
            itemDTO.setProductCode(item.getProductCode().getValue());
            itemDTO.setQuantity(item.getQuantity());
            itemDTO.setExpirationDate(item.getExpirationDate());
            return itemDTO;
        }).collect(Collectors.toList());
        dto.setLineItems(lineItemDTOs);

        return dto;
    }
}

