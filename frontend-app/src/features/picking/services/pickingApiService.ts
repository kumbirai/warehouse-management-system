import apiClient from '../../../services/apiClient';
import {
  CreatePickingListApiResponse,
  CreatePickingListRequest,
  GetLoadApiResponse,
  GetPickingListApiResponse,
  ListOrdersByLoadApiResponse,
  ListPickingListsApiResponse,
  ListPickingListsFilters,
  UploadPickingListCsvApiResponse,
  UploadPickingListCsvRequest,
} from '../types/pickingTypes';

const PICKING_BASE_PATH = '/picking';

export const pickingApiService = {
  /**
   * Uploads a CSV file containing picking list data.
   */
  async uploadPickingListCsv(
    request: UploadPickingListCsvRequest,
    tenantId: string
  ): Promise<UploadPickingListCsvApiResponse> {
    const formData = new FormData();
    formData.append('file', request.file);

    const response = await apiClient.post<UploadPickingListCsvApiResponse>(
      `${PICKING_BASE_PATH}/picking-lists/upload-csv`,
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

  /**
   * Creates a picking list manually.
   */
  async createPickingList(
    request: CreatePickingListRequest,
    tenantId: string
  ): Promise<CreatePickingListApiResponse> {
    const response = await apiClient.post<CreatePickingListApiResponse>(
      `${PICKING_BASE_PATH}/picking-lists`,
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
   * Gets a picking list by ID.
   */
  async getPickingList(
    pickingListId: string,
    tenantId: string
  ): Promise<GetPickingListApiResponse> {
    const response = await apiClient.get<GetPickingListApiResponse>(
      `${PICKING_BASE_PATH}/picking-lists/${pickingListId}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Lists picking lists with filters and pagination.
   */
  async listPickingLists(
    filters: ListPickingListsFilters,
    tenantId: string
  ): Promise<ListPickingListsApiResponse> {
    const params = new URLSearchParams();
    if (filters.page !== undefined) params.append('page', filters.page.toString());
    if (filters.size !== undefined) params.append('size', filters.size.toString());
    if (filters.status) params.append('status', filters.status);

    const response = await apiClient.get<ListPickingListsApiResponse>(
      `${PICKING_BASE_PATH}/picking-lists?${params.toString()}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Gets a load by ID.
   */
  async getLoad(loadId: string, tenantId: string): Promise<GetLoadApiResponse> {
    const response = await apiClient.get<GetLoadApiResponse>(
      `${PICKING_BASE_PATH}/loads/${loadId}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Lists orders by load ID.
   */
  async listOrdersByLoad(loadId: string, tenantId: string): Promise<ListOrdersByLoadApiResponse> {
    const response = await apiClient.get<ListOrdersByLoadApiResponse>(
      `${PICKING_BASE_PATH}/loads/${loadId}/orders`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },
};
