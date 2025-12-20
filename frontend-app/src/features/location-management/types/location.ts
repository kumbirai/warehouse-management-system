/**
 * Location Management Types
 *
 * Type definitions for Location Management feature.
 * These types match the backend DTOs for type safety.
 */

export interface LocationCoordinates {
  zone: string;
  aisle: string;
  rack: string;
  level: string;
}

export interface LocationCapacity {
  currentQuantity: number;
  maximumQuantity: number;
}

export interface CreateLocationRequest {
  zone: string;
  aisle: string;
  rack: string;
  level: string;
  barcode?: string;
  description?: string;
}

export interface CreateLocationResponse {
  locationId: string;
  barcode: string;
  coordinates: LocationCoordinates;
  status: string;
  createdAt: string;
}

export interface Location {
  locationId: string;
  code?: string;
  name?: string;
  type?: string;
  path?: string;
  barcode: string;
  coordinates: LocationCoordinates;
  status: string;
  capacity?: LocationCapacity | number; // Backend may return Integer (maxQuantity) or LocationCapacity object
  description?: string;
  createdAt: string;
  lastModifiedAt?: string;
}

export interface LocationListFilters {
  tenantId?: string;
  page?: number;
  size?: number;
  zone?: string;
  status?: string;
  search?: string;
}

export interface LocationListQueryResult {
  locations: Location[];
  totalCount: number;
  page: number;
  size: number;
}

export type LocationStatus = 'AVAILABLE' | 'OCCUPIED' | 'RESERVED' | 'BLOCKED';

export interface UpdateLocationStatusRequest {
  status: LocationStatus;
  reason?: string;
}
