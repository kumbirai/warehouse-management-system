import apiClient from '../../../services/apiClient';
import {
  CreateConsignmentApiResponse,
  CreateConsignmentRequest,
  GetConsignmentApiResponse,
  UploadConsignmentCsvApiResponse,
  UploadConsignmentCsvRequest,
  ValidateConsignmentApiResponse,
  ValidateConsignmentRequest,
} from '../types/stockManagement';

const STOCK_MANAGEMENT_BASE_PATH = '/stock-management-service/consignments';

export const stockManagementService = {
  /**
   * Creates a new stock consignment.
   */
  async createConsignment(
    request: CreateConsignmentRequest,
    tenantId: string
  ): Promise<CreateConsignmentApiResponse> {
    const response = await apiClient.post<CreateConsignmentApiResponse>(
      STOCK_MANAGEMENT_BASE_PATH,
      request,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Gets a consignment by ID.
   */
  async getConsignment(
    consignmentId: string,
    tenantId: string
  ): Promise<GetConsignmentApiResponse> {
    const response = await apiClient.get<GetConsignmentApiResponse>(
      `${STOCK_MANAGEMENT_BASE_PATH}/${consignmentId}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Validates (confirms) a consignment.
   */
  async validateConsignment(
    request: ValidateConsignmentRequest,
    tenantId: string
  ): Promise<ValidateConsignmentApiResponse> {
    const response = await apiClient.post<ValidateConsignmentApiResponse>(
      `${STOCK_MANAGEMENT_BASE_PATH}/${request.consignmentId}/validate`,
      {},
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Uploads a CSV file containing consignment data.
   */
  async uploadConsignmentCsv(
    request: UploadConsignmentCsvRequest,
    tenantId: string
  ): Promise<UploadConsignmentCsvApiResponse> {
    const formData = new FormData();
    formData.append('file', request.file);

    const response = await apiClient.post<UploadConsignmentCsvApiResponse>(
      `${STOCK_MANAGEMENT_BASE_PATH}/upload-csv`,
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
};
