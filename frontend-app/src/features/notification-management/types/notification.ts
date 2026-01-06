/**
 * Notification Management Types
 *
 * Type definitions for Notification Management feature.
 * These types match the backend DTOs for type safety.
 */

export type NotificationType =
  | 'USER_CREATED'
  | 'USER_UPDATED'
  | 'USER_DEACTIVATED'
  | 'USER_ACTIVATED'
  | 'USER_SUSPENDED'
  | 'USER_ROLE_ASSIGNED'
  | 'USER_ROLE_REMOVED'
  | 'TENANT_CREATED'
  | 'TENANT_ACTIVATED'
  | 'TENANT_DEACTIVATED'
  | 'TENANT_SUSPENDED'
  | 'WELCOME'
  | 'SYSTEM_ALERT';

export type NotificationStatus = 'PENDING' | 'SENT' | 'DELIVERED' | 'FAILED' | 'READ';

export interface Notification {
  notificationId: string;
  tenantId: string;
  recipientUserId: string;
  title: string;
  message: string;
  type: NotificationType;
  status: NotificationStatus;
  createdAt: string;
  lastModifiedAt?: string;
  sentAt?: string;
  readAt?: string;
}

export interface NotificationListFilters {
  tenantId?: string;
  recipientUserId?: string;
  status?: NotificationStatus;
  type?: NotificationType;
  page?: number;
  size?: number;
}

export interface NotificationListResponse {
  items: Notification[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
