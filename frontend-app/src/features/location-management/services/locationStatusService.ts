import apiClient from '../../../services/apiClient';
import {ApiResponse} from '../../../types/api';
import {BlockLocationRequest} from '../types/location';

/**
 * Location Status Service
 *
 * Handles API calls for location status operations (block/unblock).
 */

export interface BlockLocationResponse {
  locationId: string;
  status: string;
}

export interface UnblockLocationResponse {
  locationId: string;
  status: string;
}

const BASE_PATH = '/location-management/locations';

export const locationStatusService = {
  /**
   * Blocks a location.
   * @param locationId - The location ID to block
   * @param reason - Required reason for blocking the location (for audit trail)
   * @param tenantId - The tenant ID
   */
  async blockLocation(
    locationId: string,
    reason: string,
    tenantId: string
  ): Promise<ApiResponse<BlockLocationResponse>> {
    const request: BlockLocationRequest = { reason };
    const response = await apiClient.post<ApiResponse<BlockLocationResponse>>(
      `${BASE_PATH}/${locationId}/block`,
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
   * Unblocks a location.
   */
  async unblockLocation(
    locationId: string,
    tenantId: string
  ): Promise<ApiResponse<UnblockLocationResponse>> {
    const response = await apiClient.post<ApiResponse<UnblockLocationResponse>>(
      `${BASE_PATH}/${locationId}/unblock`,
      {},
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },
};
