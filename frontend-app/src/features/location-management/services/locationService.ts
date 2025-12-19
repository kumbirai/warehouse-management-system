import apiClient from '../../../services/apiClient';
import { ApiResponse } from '../../../types/api';
import {
  CreateLocationRequest,
  CreateLocationResponse,
  Location,
  LocationListFilters,
  LocationListQueryResult,
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

  async listLocations(filters: LocationListFilters): Promise<LocationListApiResponse> {
    const headers: Record<string, string> = {};
    if (filters.tenantId) {
      headers['X-Tenant-Id'] = filters.tenantId;
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
      logger.debug('Location list response:', {
        status: response.status,
        hasData: !!response.data,
        locationsLength: Array.isArray(response.data?.data?.locations)
          ? response.data.data.locations.length
          : 'N/A',
        totalCount: response.data?.data?.totalCount ?? 'N/A',
      });
    }

    return response.data;
  },
};
