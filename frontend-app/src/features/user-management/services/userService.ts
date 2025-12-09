import apiClient from '../../../services/apiClient';
import {ApiResponse} from '../../../types/api';
import {AssignRoleRequest, CreateUserRequest, CreateUserResponse, UpdateUserProfileRequest, User, UserListFilters,} from '../types/user';
import {logger} from '../../../utils/logger';

const USER_BASE_PATH = '/users';

export interface UserListApiResponse extends ApiResponse<User[]> {
}

export const userService = {
    async listUsers(filters: UserListFilters): Promise<UserListApiResponse> {
        const response = await apiClient.get<UserListApiResponse>(USER_BASE_PATH, {
            params: {
                page: filters.page,
                size: filters.size,
                tenantId: filters.tenantId,
                status: filters.status,
                search: filters.search,
            },
        });

        // Debug logging to see what we're receiving
        if (import.meta.env.DEV) {
            logger.debug('User list raw axios response:', {
                status: response.status,
                statusText: response.statusText,
                headers: response.headers,
                hasData: !!response.data,
                dataType: typeof response.data,
                isArray: Array.isArray(response.data?.data),
                dataLength: Array.isArray(response.data?.data) ? response.data.data.length : 'N/A',
                dataKeys: response.data ? Object.keys(response.data) : [],
                fullResponse: JSON.stringify(response.data, null, 2),
            });
        }

        return response.data;
    },

    async getUser(userId: string): Promise<ApiResponse<User>> {
        const response = await apiClient.get<ApiResponse<User>>(`${USER_BASE_PATH}/${userId}`);
        return response.data;
    },

    async createUser(request: CreateUserRequest): Promise<ApiResponse<CreateUserResponse>> {
        const response = await apiClient.post<ApiResponse<CreateUserResponse>>(
            USER_BASE_PATH,
            request
        );
        return response.data;
    },

    async updateUserProfile(userId: string, request: UpdateUserProfileRequest): Promise<ApiResponse<void>> {
        const response = await apiClient.put<ApiResponse<void>>(
            `${USER_BASE_PATH}/${userId}/profile`,
            request
        );
        return response.data;
    },

    async activateUser(userId: string): Promise<ApiResponse<void>> {
        const response = await apiClient.put<ApiResponse<void>>(
            `${USER_BASE_PATH}/${userId}/activate`
        );
        return response.data;
    },

    async deactivateUser(userId: string): Promise<ApiResponse<void>> {
        const response = await apiClient.put<ApiResponse<void>>(
            `${USER_BASE_PATH}/${userId}/deactivate`
        );
        return response.data;
    },

    async suspendUser(userId: string): Promise<ApiResponse<void>> {
        const response = await apiClient.put<ApiResponse<void>>(
            `${USER_BASE_PATH}/${userId}/suspend`
        );
        return response.data;
    },

    async assignRole(userId: string, request: AssignRoleRequest): Promise<ApiResponse<void>> {
        const response = await apiClient.post<ApiResponse<void>>(
            `${USER_BASE_PATH}/${userId}/roles`,
            request
        );
        return response.data;
    },

    async removeRole(userId: string, roleId: string): Promise<ApiResponse<void>> {
        const response = await apiClient.delete<ApiResponse<void>>(
            `${USER_BASE_PATH}/${userId}/roles/${roleId}`
        );
        return response.data;
    },

    async getUserRoles(userId: string): Promise<ApiResponse<string[]>> {
        const response = await apiClient.get<ApiResponse<string[]>>(
            `${USER_BASE_PATH}/${userId}/roles`
        );
        return response.data;
    },
};

