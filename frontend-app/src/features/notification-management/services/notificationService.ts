import apiClient from '../../../services/apiClient';
import { ApiResponse } from '../../../types/api';
import { Notification, NotificationListFilters } from '../types/notification';

const BASE_PATH = '/notifications';

export interface NotificationListApiResponse extends ApiResponse<Notification[]> {}

export const notificationService = {
  /**
   * Lists notifications with optional filters.
   * Note: Backend returns a list directly, not paginated response.
   * We'll handle pagination on the frontend.
   */
  async listNotifications(
    filters: NotificationListFilters
  ): Promise<NotificationListApiResponse> {
    const headers: Record<string, string> = {};
    if (filters.tenantId) {
      headers['X-Tenant-Id'] = filters.tenantId;
    }

    const params: Record<string, string | number> = {};
    if (filters.recipientUserId) {
      params.recipientUserId = filters.recipientUserId;
    }
    if (filters.status) {
      params.status = filters.status;
    }
    if (filters.type) {
      params.type = filters.type;
    }
    if (filters.page !== undefined) {
      params.page = filters.page;
    }
    if (filters.size !== undefined) {
      params.size = filters.size;
    }

    const response = await apiClient.get<NotificationListApiResponse>(BASE_PATH, {
      params,
      headers,
    });

    return response.data;
  },

  /**
   * Gets a notification by ID.
   */
  async getNotification(
    notificationId: string,
    tenantId: string
  ): Promise<ApiResponse<Notification>> {
    const response = await apiClient.get<ApiResponse<Notification>>(
      `${BASE_PATH}/${notificationId}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Marks a notification as read.
   */
  async markAsRead(
    notificationId: string,
    tenantId: string
  ): Promise<ApiResponse<Notification>> {
    const response = await apiClient.put<ApiResponse<Notification>>(
      `${BASE_PATH}/${notificationId}/mark-as-read`,
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
