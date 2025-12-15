export type TenantStatus = 'PENDING' | 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

export interface Tenant {
  tenantId: string;
  name: string;
  status: TenantStatus;
  emailAddress?: string | null;
  phone?: string | null;
  address?: string | null;
  keycloakRealmName?: string | null;
  usePerTenantRealm: boolean;
  createdAt: string;
  activatedAt?: string | null;
  deactivatedAt?: string | null;
}

export interface TenantListFilters {
  page: number;
  size: number;
  status?: TenantStatus;
  search?: string;
}

export interface CreateTenantRequest {
  tenantId: string;
  name: string;
  emailAddress?: string;
  phone?: string;
  address?: string;
  keycloakRealmName?: string;
  usePerTenantRealm?: boolean;
}

export interface CreateTenantResponse {
  tenantId: string;
  success: boolean;
  message?: string;
}
