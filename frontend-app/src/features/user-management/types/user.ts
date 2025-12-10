export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

export interface User {
  userId: string;
  tenantId: string;
  tenantName?: string;
  username: string;
  emailAddress: string;
  firstName?: string;
  lastName?: string;
  status: UserStatus;
  keycloakUserId?: string;
  roles: string[];
  createdAt: string;
  lastModifiedAt?: string;
}

export interface UserListFilters {
  page: number;
  size: number;
  tenantId?: string;
  status?: UserStatus;
  search?: string;
}

export interface CreateUserRequest {
  tenantId: string;
  username: string;
  emailAddress: string;
  firstName?: string;
  lastName?: string;
  password: string;
  roles?: string[];
}

export interface CreateUserResponse {
  userId: string;
  success: boolean;
  message?: string;
}

export interface UpdateUserProfileRequest {
  emailAddress: string;
  firstName?: string;
  lastName?: string;
}

export interface AssignRoleRequest {
  roleId: string;
}
