import apiClient from '../../../services/apiClient';
import { ApiResponse } from '../../../types/api';
import {
  AssignLocationsFEFORequest,
  CreateLocationRequest,
  CreateLocationResponse,
  Location,
  LocationHierarchyQueryResult,
  LocationListFilters,
  LocationListQueryResult,
  UpdateLocationRequest,
  UpdateLocationStatusRequest,
} from '../types/location';
import { logger } from '../../../utils/logger';

const LOCATION_BASE_PATH = '/location-management/locations';

export interface LocationListApiResponse extends ApiResponse<LocationListQueryResult> {}

export const locationService = {
  async createLocation(
    request: CreateLocationRequest,
    tenantId: string
  ): Promise<ApiResponse<CreateLocationResponse>> {
    const response = await apiClient.post<ApiResponse<CreateLocationResponse>>(
      LOCATION_BASE_PATH,
      request,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  async getLocation(locationId: string, tenantId: string): Promise<ApiResponse<Location>> {
    const response = await apiClient.get<ApiResponse<Location>>(
      `${LOCATION_BASE_PATH}/${locationId}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  async getWarehouse(warehouseId: string, tenantId: string): Promise<ApiResponse<Location>> {
    const response = await apiClient.get<ApiResponse<Location>>(
      `/location-management/warehouses/${warehouseId}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  async listLocations(filters: LocationListFilters): Promise<LocationListApiResponse> {
    const headers: Record<string, string> = {};
    if (filters.tenantId) {
      headers['X-Tenant-Id'] = filters.tenantId;
    }

    if (import.meta.env.DEV) {
      logger.debug('Calling listLocations API:', {
        path: LOCATION_BASE_PATH,
        params: {
          page: filters.page,
          size: filters.size,
          zone: filters.zone,
          status: filters.status,
          search: filters.search,
        },
        headers: {
          'X-Tenant-Id': filters.tenantId,
        },
      });
    }

    const response = await apiClient.get<LocationListApiResponse>(LOCATION_BASE_PATH, {
      params: {
        page: filters.page,
        size: filters.size,
        zone: filters.zone,
        status: filters.status,
        search: filters.search,
      },
      headers,
    });

    if (import.meta.env.DEV) {
      logger.debug('Location list raw axios response:', {
        status: response.status,
        statusText: response.statusText,
        hasData: !!response.data,
        dataType: typeof response.data,
        dataKeys: response.data ? Object.keys(response.data) : [],
        hasError: !!response.data?.error,
        hasDataField: !!response.data?.data,
        locationsLength: Array.isArray(response.data?.data?.locations)
          ? response.data.data.locations.length
          : 'N/A',
        totalCount: response.data?.data?.totalCount ?? 'N/A',
        // Log full response structure for debugging
        responseStructure: JSON.stringify(response.data, null, 2).substring(0, 500),
      });
    }

    return response.data;
  },

  async updateLocation(
    locationId: string,
    request: UpdateLocationRequest,
    tenantId: string
  ): Promise<ApiResponse<Location>> {
    const response = await apiClient.put<ApiResponse<Location>>(
      `${LOCATION_BASE_PATH}/${locationId}`,
      request,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  async updateLocationStatus(
    locationId: string,
    request: UpdateLocationStatusRequest,
    tenantId: string
  ): Promise<ApiResponse<Location>> {
    const response = await apiClient.put<ApiResponse<Location>>(
      `${LOCATION_BASE_PATH}/${locationId}/status`,
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
   * Assigns locations to stock items using FEFO (First-Expiring-First-Out) algorithm.
   */
  async assignLocationsFEFO(
    request: AssignLocationsFEFORequest,
    tenantId: string
  ): Promise<ApiResponse<void>> {
    const response = await apiClient.post<ApiResponse<void>>(
      `${LOCATION_BASE_PATH}/assign-fefo`,
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
   * Lists all warehouses (locations with type WAREHOUSE).
   */
  async listWarehouses(tenantId: string): Promise<ApiResponse<LocationHierarchyQueryResult>> {
    const response = await apiClient.get<ApiResponse<LocationHierarchyQueryResult>>(
      `${LOCATION_BASE_PATH}/warehouses`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Lists zones under a warehouse.
   */
  async listZones(
    warehouseId: string,
    tenantId: string
  ): Promise<ApiResponse<LocationHierarchyQueryResult>> {
    const response = await apiClient.get<ApiResponse<LocationHierarchyQueryResult>>(
      `${LOCATION_BASE_PATH}/warehouses/${warehouseId}/zones`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Lists aisles under a zone.
   */
  async listAisles(
    zoneId: string,
    tenantId: string
  ): Promise<ApiResponse<LocationHierarchyQueryResult>> {
    const response = await apiClient.get<ApiResponse<LocationHierarchyQueryResult>>(
      `${LOCATION_BASE_PATH}/zones/${zoneId}/aisles`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Lists racks under an aisle.
   */
  async listRacks(
    aisleId: string,
    tenantId: string
  ): Promise<ApiResponse<LocationHierarchyQueryResult>> {
    const response = await apiClient.get<ApiResponse<LocationHierarchyQueryResult>>(
      `${LOCATION_BASE_PATH}/aisles/${aisleId}/racks`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Lists bins under a rack.
   */
  async listBins(
    rackId: string,
    tenantId: string
  ): Promise<ApiResponse<LocationHierarchyQueryResult>> {
    const response = await apiClient.get<ApiResponse<LocationHierarchyQueryResult>>(
      `${LOCATION_BASE_PATH}/racks/${rackId}/bins`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },
};
