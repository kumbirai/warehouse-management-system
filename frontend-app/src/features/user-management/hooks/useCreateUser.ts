import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userService } from '../services/userService';
import { CreateUserRequest } from '../types/user';

export const useCreateUser = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const navigate = useNavigate();

  const createUser = async (request: CreateUserRequest) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await userService.createUser(request);
      if (response.error) {
        throw new Error(response.error.message);
      }

      // Redirect to user detail page
      setTimeout(() => {
        navigate(`/admin/users/${response.data.userId}`);
      }, 1500);

      return response.data;
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to create user');
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { createUser, isLoading, error };
};
