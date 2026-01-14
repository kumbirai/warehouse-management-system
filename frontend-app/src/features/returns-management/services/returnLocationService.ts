import apiClient from '../../../services/apiClient';
import { ApiResponse } from '../../../types/api';
import { logger } from '../../../utils/logger';

const LOCATION_MANAGEMENT_BASE_PATH = '/location-management';

export interface AssignReturnLocationsRequest {
  returnId: string;
}

export interface AssignReturnLocationsResponse {
  returnId: string;
  assignments: Record<string, string>; // lineItemId -> locationId
}

export const returnLocationService = {
  async assignReturnLocations(
    request: AssignReturnLocationsRequest,
    tenantId: string
  ): Promise<ApiResponse<AssignReturnLocationsResponse>> {
    if (import.meta.env.DEV) {
      logger.debug('Calling assignReturnLocations API:', {
        path: `${LOCATION_MANAGEMENT_BASE_PATH}/returns/assign-locations`,
        request,
        tenantId,
      });
    }

    const response = await apiClient.post<ApiResponse<AssignReturnLocationsResponse>>(
      `${LOCATION_MANAGEMENT_BASE_PATH}/returns/assign-locations`,
      request,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );

    if (import.meta.env.DEV) {
      logger.debug('Assign return locations response:', {
        status: response.status,
        data: response.data,
      });
    }

    return response.data;
  },
};
