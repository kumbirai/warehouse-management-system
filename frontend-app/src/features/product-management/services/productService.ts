import apiClient from '../../../services/apiClient';
import { ApiResponse } from '../../../types/api';
import {
  CreateProductRequest,
  CreateProductResponse,
  UpdateProductRequest,
  UpdateProductResponse,
  Product,
  ProductListFilters,
  UploadProductCsvResponse,
  ProductCodeUniquenessResponse,
} from '../types/product';
import { logger } from '../../../utils/logger';

const PRODUCT_BASE_PATH = '/product-service/products';

export interface ProductListApiResponse extends ApiResponse<Product[]> {}

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
        dataLength: Array.isArray(response.data?.data) ? response.data.data.length : 'N/A',
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
};

