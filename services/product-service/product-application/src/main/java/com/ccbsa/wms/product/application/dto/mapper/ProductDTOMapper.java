package com.ccbsa.wms.product.application.dto.mapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.application.dto.command.CreateProductCommandDTO;
import com.ccbsa.wms.product.application.dto.command.CreateProductResultDTO;
import com.ccbsa.wms.product.application.dto.command.ProductCsvErrorDTO;
import com.ccbsa.wms.product.application.dto.command.UpdateProductCommandDTO;
import com.ccbsa.wms.product.application.dto.command.UpdateProductResultDTO;
import com.ccbsa.wms.product.application.dto.command.UploadProductCsvResultDTO;
import com.ccbsa.wms.product.application.dto.query.ListProductsQueryResultDTO;
import com.ccbsa.wms.product.application.dto.query.ProductCodeUniquenessResultDTO;
import com.ccbsa.wms.product.application.dto.query.ProductQueryResultDTO;
import com.ccbsa.wms.product.application.dto.query.ValidateProductBarcodeResultDTO;
import com.ccbsa.wms.product.application.service.command.dto.CreateProductCommand;
import com.ccbsa.wms.product.application.service.command.dto.CreateProductResult;
import com.ccbsa.wms.product.application.service.command.dto.ProductCsvError;
import com.ccbsa.wms.product.application.service.command.dto.UpdateProductCommand;
import com.ccbsa.wms.product.application.service.command.dto.UpdateProductResult;
import com.ccbsa.wms.product.application.service.command.dto.UploadProductCsvCommand;
import com.ccbsa.wms.product.application.service.command.dto.UploadProductCsvResult;
import com.ccbsa.wms.product.application.service.query.dto.CheckProductCodeUniquenessQuery;
import com.ccbsa.wms.product.application.service.query.dto.GetProductByCodeQuery;
import com.ccbsa.wms.product.application.service.query.dto.GetProductQuery;
import com.ccbsa.wms.product.application.service.query.dto.ListProductsQuery;
import com.ccbsa.wms.product.application.service.query.dto.ListProductsQueryResult;
import com.ccbsa.wms.product.application.service.query.dto.ProductCodeUniquenessResult;
import com.ccbsa.wms.product.application.service.query.dto.ProductInfo;
import com.ccbsa.wms.product.application.service.query.dto.ProductQueryResult;
import com.ccbsa.wms.product.application.service.query.dto.ValidateProductBarcodeQuery;
import com.ccbsa.wms.product.application.service.query.dto.ValidateProductBarcodeResult;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductId;
import com.ccbsa.wms.product.domain.core.valueobject.UnitOfMeasure;

/**
 * DTO Mapper: ProductDTOMapper
 * <p>
 * Maps between API DTOs and application service commands/queries. Acts as an anti-corruption layer protecting the domain from external API changes.
 */
@Component
public class ProductDTOMapper {

    /**
     * Converts CreateProductCommandDTO to CreateProductCommand.
     *
     * @param dto      Command DTO
     * @param tenantId Tenant identifier string
     * @return CreateProductCommand
     */
    public CreateProductCommand toCreateCommand(CreateProductCommandDTO dto, String tenantId) {
        CreateProductCommand.Builder builder =
                CreateProductCommand.builder().tenantId(TenantId.of(tenantId)).productCode(ProductCode.of(dto.getProductCode())).description(dto.getDescription())
                        .primaryBarcode(ProductBarcode.of(dto.getPrimaryBarcode())).unitOfMeasure(UnitOfMeasure.valueOf(dto.getUnitOfMeasure()));

        // Add secondary barcodes if provided
        if (dto.getSecondaryBarcodes() != null && !dto.getSecondaryBarcodes().isEmpty()) {
            List<ProductBarcode> secondaryBarcodes = dto.getSecondaryBarcodes().stream().map(ProductBarcode::of).collect(Collectors.toList());
            builder.secondaryBarcodes(secondaryBarcodes);
        }

        // Set optional fields
        if (dto.getCategory() != null && !dto.getCategory().trim().isEmpty()) {
            builder.category(dto.getCategory());
        }
        if (dto.getBrand() != null && !dto.getBrand().trim().isEmpty()) {
            builder.brand(dto.getBrand());
        }

        return builder.build();
    }

    /**
     * Converts CreateProductResult to CreateProductResultDTO.
     *
     * @param result Command result
     * @return CreateProductResultDTO
     */
    public CreateProductResultDTO toCreateResultDTO(CreateProductResult result) {
        CreateProductResultDTO dto = new CreateProductResultDTO();
        dto.setProductId(result.getProductId().getValueAsString());
        dto.setProductCode(result.getProductCode().getValue());
        dto.setDescription(result.getDescription());
        dto.setPrimaryBarcode(result.getPrimaryBarcode().getValue());
        dto.setCreatedAt(result.getCreatedAt());
        return dto;
    }

    /**
     * Converts UpdateProductCommandDTO to UpdateProductCommand.
     *
     * @param dto       Command DTO
     * @param productId Product ID string
     * @param tenantId  Tenant identifier string
     * @return UpdateProductCommand
     */
    public UpdateProductCommand toUpdateCommand(UpdateProductCommandDTO dto, String productId, String tenantId) {
        UpdateProductCommand.Builder builder =
                UpdateProductCommand.builder().productId(ProductId.of(UUID.fromString(productId))).tenantId(TenantId.of(tenantId)).description(dto.getDescription())
                        .primaryBarcode(ProductBarcode.of(dto.getPrimaryBarcode())).unitOfMeasure(UnitOfMeasure.valueOf(dto.getUnitOfMeasure()));

        // Add secondary barcodes if provided
        if (dto.getSecondaryBarcodes() != null && !dto.getSecondaryBarcodes().isEmpty()) {
            List<ProductBarcode> secondaryBarcodes = dto.getSecondaryBarcodes().stream().map(ProductBarcode::of).collect(Collectors.toList());
            builder.secondaryBarcodes(secondaryBarcodes);
        }

        // Set optional fields
        if (dto.getCategory() != null && !dto.getCategory().trim().isEmpty()) {
            builder.category(dto.getCategory());
        }
        if (dto.getBrand() != null && !dto.getBrand().trim().isEmpty()) {
            builder.brand(dto.getBrand());
        }

        return builder.build();
    }

    /**
     * Converts UpdateProductResult to UpdateProductResultDTO.
     *
     * @param result Command result
     * @return UpdateProductResultDTO
     */
    public UpdateProductResultDTO toUpdateResultDTO(UpdateProductResult result) {
        UpdateProductResultDTO dto = new UpdateProductResultDTO();
        dto.setProductId(result.getProductId().getValueAsString());
        dto.setLastModifiedAt(result.getLastModifiedAt());
        return dto;
    }

    /**
     * Converts MultipartFile to UploadProductCsvCommand.
     *
     * @param file     CSV file
     * @param tenantId Tenant identifier string
     * @return UploadProductCsvCommand
     * @throws IllegalArgumentException if file is invalid
     */
    public UploadProductCsvCommand toUploadCsvCommand(MultipartFile file, String tenantId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required");
        }

        // Validate file size (10MB limit)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }

        // Read file content
        String csvContent;
        try {
            csvContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Failed to read CSV file: %s", e.getMessage()), e);
        }

        return UploadProductCsvCommand.builder().tenantId(TenantId.of(tenantId)).csvContent(csvContent).fileName(file.getOriginalFilename()).build();
    }

    /**
     * Converts UploadProductCsvResult to UploadProductCsvResultDTO.
     *
     * @param result Command result
     * @return UploadProductCsvResultDTO
     */
    public UploadProductCsvResultDTO toUploadCsvResultDTO(UploadProductCsvResult result) {
        UploadProductCsvResultDTO dto = new UploadProductCsvResultDTO();
        dto.setTotalRows(result.getTotalRows());
        dto.setCreatedCount(result.getCreatedCount());
        dto.setUpdatedCount(result.getUpdatedCount());
        dto.setErrorCount(result.getErrorCount());

        // Map errors
        if (result.getErrors() != null && !result.getErrors().isEmpty()) {
            List<ProductCsvErrorDTO> errorDTOs = result.getErrors().stream().map(this::toCsvErrorDTO).collect(Collectors.toList());
            dto.setErrors(errorDTOs);
        } else {
            dto.setErrors(new ArrayList<>());
        }

        return dto;
    }

    /**
     * Converts ProductCsvError to ProductCsvErrorDTO.
     *
     * @param error Error object
     * @return ProductCsvErrorDTO
     */
    private ProductCsvErrorDTO toCsvErrorDTO(ProductCsvError error) {
        ProductCsvErrorDTO dto = new ProductCsvErrorDTO();
        dto.setRowNumber(error.getRowNumber());
        dto.setProductCode(error.getProductCode());
        dto.setErrorMessage(error.getErrorMessage());
        return dto;
    }

    /**
     * Converts product ID string and tenant ID to GetProductQuery.
     *
     * @param productId Product ID string
     * @param tenantId  Tenant ID string
     * @return GetProductQuery
     */
    public GetProductQuery toGetProductQuery(String productId, String tenantId) {
        return GetProductQuery.builder().productId(ProductId.of(UUID.fromString(productId))).tenantId(TenantId.of(tenantId)).build();
    }

    /**
     * Converts product code and tenant ID to GetProductByCodeQuery.
     *
     * @param productCode Product code string
     * @param tenantId    Tenant ID string
     * @return GetProductByCodeQuery
     */
    public GetProductByCodeQuery toGetProductByCodeQuery(String productCode, String tenantId) {
        return GetProductByCodeQuery.builder().productCode(ProductCode.of(productCode)).tenantId(TenantId.of(tenantId)).build();
    }

    /**
     * Converts product code and tenant ID to CheckProductCodeUniquenessQuery.
     *
     * @param productCode Product code string
     * @param tenantId    Tenant ID string
     * @return CheckProductCodeUniquenessQuery
     */
    public CheckProductCodeUniquenessQuery toCheckProductCodeUniquenessQuery(String productCode, String tenantId) {
        return CheckProductCodeUniquenessQuery.builder().productCode(ProductCode.of(productCode)).tenantId(TenantId.of(tenantId)).build();
    }

    /**
     * Converts ProductCodeUniquenessResult to ProductCodeUniquenessResultDTO.
     *
     * @param result Query result
     * @return ProductCodeUniquenessResultDTO
     */
    public ProductCodeUniquenessResultDTO toProductCodeUniquenessResultDTO(ProductCodeUniquenessResult result) {
        ProductCodeUniquenessResultDTO dto = new ProductCodeUniquenessResultDTO();
        dto.setProductCode(result.getProductCode().getValue());
        dto.setUnique(result.isUnique());
        return dto;
    }

    /**
     * Converts barcode string and tenant ID to ValidateProductBarcodeQuery.
     *
     * @param barcode  Barcode string
     * @param tenantId Tenant ID string
     * @return ValidateProductBarcodeQuery
     */
    public ValidateProductBarcodeQuery toValidateProductBarcodeQuery(String barcode, String tenantId) {
        return ValidateProductBarcodeQuery.builder().barcode(barcode).tenantId(TenantId.of(tenantId)).build();
    }

    /**
     * Converts ValidateProductBarcodeResult to ValidateProductBarcodeResultDTO.
     *
     * @param result Query result
     * @return ValidateProductBarcodeResultDTO
     */
    public ValidateProductBarcodeResultDTO toValidateProductBarcodeResultDTO(ValidateProductBarcodeResult result) {
        ValidateProductBarcodeResultDTO dto = new ValidateProductBarcodeResultDTO();
        dto.setValid(result.isValid());
        dto.setBarcodeFormat(result.getBarcodeFormat());
        dto.setErrorMessage(result.getErrorMessage());

        if (result.getProductInfo() != null) {
            ProductInfo productInfo = result.getProductInfo();
            ValidateProductBarcodeResultDTO.ProductInfoDTO productInfoDTO = new ValidateProductBarcodeResultDTO.ProductInfoDTO();
            productInfoDTO.setProductId(productInfo.getProductId().getValueAsString());
            productInfoDTO.setProductCode(productInfo.getProductCode().getValue());
            productInfoDTO.setDescription(productInfo.getDescription());
            productInfoDTO.setBarcode(productInfo.getBarcode().getValue());
            productInfoDTO.setBarcodeType(productInfo.getBarcode().getType());
            dto.setProductInfo(productInfoDTO);
        }

        return dto;
    }

    /**
     * Converts request parameters to ListProductsQuery.
     *
     * @param tenantId Tenant ID string
     * @param page     Page number (optional)
     * @param size     Page size (optional)
     * @param category Category filter (optional)
     * @param brand    Brand filter (optional)
     * @param search   Search term (optional)
     * @return ListProductsQuery
     */
    public ListProductsQuery toListProductsQuery(String tenantId, Integer page, Integer size, String category, String brand, String search) {
        ListProductsQuery.Builder builder = ListProductsQuery.builder().tenantId(TenantId.of(tenantId));

        if (page != null) {
            builder.page(page);
        }
        if (size != null) {
            builder.size(size);
        }
        if (category != null && !category.trim().isEmpty()) {
            builder.category(category.trim());
        }
        if (brand != null && !brand.trim().isEmpty()) {
            builder.brand(brand.trim());
        }
        if (search != null && !search.trim().isEmpty()) {
            builder.search(search.trim());
        }

        return builder.build();
    }

    /**
     * Converts ListProductsQueryResult to ListProductsQueryResultDTO.
     *
     * @param result Query result
     * @return ListProductsQueryResultDTO
     */
    public ListProductsQueryResultDTO toListProductsQueryResultDTO(ListProductsQueryResult result) {
        ListProductsQueryResultDTO dto = new ListProductsQueryResultDTO();

        // Map products
        List<ProductQueryResultDTO> productDTOs = result.getProducts().stream().map(this::toQueryResultDTO).collect(Collectors.toList());
        dto.setProducts(productDTOs);

        dto.setTotalCount(result.getTotalCount());
        dto.setPage(result.getPage());
        dto.setSize(result.getSize());

        return dto;
    }

    /**
     * Converts ProductQueryResult to ProductQueryResultDTO.
     *
     * @param result Query result
     * @return ProductQueryResultDTO
     */
    public ProductQueryResultDTO toQueryResultDTO(ProductQueryResult result) {
        ProductQueryResultDTO dto = new ProductQueryResultDTO();
        dto.setProductId(result.getProductId().getValueAsString());
        dto.setProductCode(result.getProductCode().getValue());
        dto.setDescription(result.getDescription());
        dto.setPrimaryBarcode(result.getPrimaryBarcode().getValue());
        dto.setUnitOfMeasure(result.getUnitOfMeasure().name());

        // Map secondary barcodes
        if (result.getSecondaryBarcodes() != null && !result.getSecondaryBarcodes().isEmpty()) {
            List<String> secondaryBarcodes = result.getSecondaryBarcodes().stream().map(ProductBarcode::getValue).collect(Collectors.toList());
            dto.setSecondaryBarcodes(secondaryBarcodes);
        } else {
            dto.setSecondaryBarcodes(new ArrayList<>());
        }

        dto.setCategory(result.getCategory());
        dto.setBrand(result.getBrand());
        dto.setCreatedAt(result.getCreatedAt());
        dto.setLastModifiedAt(result.getLastModifiedAt());
        return dto;
    }
}

