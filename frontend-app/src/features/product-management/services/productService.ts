import apiClient from '../../../services/apiClient';
import { ApiResponse } from '../../../types/api';
import {
  CreateProductRequest,
  CreateProductResponse,
  Product,
  ProductCodeUniquenessResponse,
  ProductListFilters,
  ProductListQueryResult,
  UpdateProductRequest,
  UpdateProductResponse,
  UploadProductCsvResponse,
} from '../types/product';
import { logger } from '../../../utils/logger';

const PRODUCT_BASE_PATH = '/products';

export interface ProductListApiResponse extends ApiResponse<ProductListQueryResult> {}

export const productService = {
  async createProduct(
    request: CreateProductRequest,
    tenantId: string
  ): Promise<ApiResponse<CreateProductResponse>> {
    const response = await apiClient.post<ApiResponse<CreateProductResponse>>(
      PRODUCT_BASE_PATH,
      request,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  async updateProduct(
    productId: string,
    request: UpdateProductRequest,
    tenantId: string
  ): Promise<ApiResponse<UpdateProductResponse>> {
    const response = await apiClient.put<ApiResponse<UpdateProductResponse>>(
      `${PRODUCT_BASE_PATH}/${productId}`,
      request,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  async getProduct(productId: string, tenantId: string): Promise<ApiResponse<Product>> {
    const response = await apiClient.get<ApiResponse<Product>>(
      `${PRODUCT_BASE_PATH}/${productId}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  async listProducts(filters: ProductListFilters): Promise<ProductListApiResponse> {
    const headers: Record<string, string> = {};
    if (filters.tenantId) {
      headers['X-Tenant-Id'] = filters.tenantId;
    }

    const response = await apiClient.get<ProductListApiResponse>(PRODUCT_BASE_PATH, {
      params: {
        page: filters.page,
        size: filters.size,
        category: filters.category,
        brand: filters.brand,
        search: filters.search,
      },
      headers,
    });

    if (import.meta.env.DEV) {
      logger.debug('Product list response:', {
        status: response.status,
        hasData: !!response.data,
        productsLength: Array.isArray(response.data?.data?.products)
          ? response.data.data.products.length
          : 'N/A',
        totalCount: response.data?.data?.totalCount ?? 'N/A',
      });
    }

    return response.data;
  },

  async uploadProductCsv(
    file: File,
    tenantId: string
  ): Promise<ApiResponse<UploadProductCsvResponse>> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await apiClient.post<ApiResponse<UploadProductCsvResponse>>(
      `${PRODUCT_BASE_PATH}/upload-csv`,
      formData,
      {
        headers: {
          'X-Tenant-Id': tenantId,
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data;
  },

  async checkProductCodeUniqueness(
    productCode: string,
    tenantId: string
  ): Promise<ApiResponse<ProductCodeUniquenessResponse>> {
    const response = await apiClient.get<ApiResponse<ProductCodeUniquenessResponse>>(
      `${PRODUCT_BASE_PATH}/check-uniqueness`,
      {
        params: {
          productCode,
        },
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Validates a product barcode.
   */
  async validateBarcode(
    barcode: string,
    tenantId: string
  ): Promise<
    ApiResponse<{
      valid: boolean;
      productInfo?: {
        productId: string;
        productCode: string;
        description: string;
        primaryBarcode: string;
      };
      barcodeFormat: string;
    }>
  > {
    const response = await apiClient.get<
      ApiResponse<{
        valid: boolean;
        productInfo?: {
          productId: string;
          productCode: string;
          description: string;
          primaryBarcode: string;
        };
        barcodeFormat: string;
      }>
    >(`${PRODUCT_BASE_PATH}/validate-barcode`, {
      params: {
        barcode,
      },
      headers: {
        'X-Tenant-Id': tenantId,
      },
    });
    return response.data;
  },
};
