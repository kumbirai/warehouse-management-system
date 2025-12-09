import {useState} from 'react';
import {userService} from '../services/userService';
import {UpdateUserProfileRequest} from '../types/user';

export const useUpdateUserProfile = (userId: string) => {
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<Error | null>(null);

    const updateProfile = async (request: UpdateUserProfileRequest) => {
        setIsLoading(true);
        setError(null);
        try {
            const response = await userService.updateUserProfile(userId, request);
            if (response.error) {
                throw new Error(response.error.message);
            }

            return true;
        } catch (err) {
            const error = err instanceof Error ? err : new Error('Failed to update profile');
            setError(error);
            throw error;
        } finally {
            setIsLoading(false);
        }
    };

    return {updateProfile, isLoading, error};
};

