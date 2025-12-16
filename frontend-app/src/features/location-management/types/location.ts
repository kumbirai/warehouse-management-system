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
  barcode: string;
  coordinates: LocationCoordinates;
  status: string;
  capacity?: LocationCapacity;
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
