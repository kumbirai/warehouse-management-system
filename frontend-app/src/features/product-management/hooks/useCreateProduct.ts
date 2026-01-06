import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { productService } from '../services/productService';
import { CreateProductRequest } from '../types/product';
import { logger } from '../../../utils/logger';
import { useToast } from '../../../hooks/useToast';

export interface UseCreateProductResult {
  createProduct: (request: CreateProductRequest, tenantId: string) => Promise<void>;
  isLoading: boolean;
  error: Error | null;
}

export const useCreateProduct = (): UseCreateProductResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const navigate = useNavigate();
  const { success, error: showErrorToast } = useToast();

  const createProduct = async (request: CreateProductRequest, tenantId: string) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await productService.createProduct(request, tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to create product');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      logger.info('Product created successfully', { productId: response.data.productId });
      success('Product created successfully');
      navigate(`/products/${response.data.productId}`);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to create product');
      logger.error('Error creating product:', error);
      setError(error);
      showErrorToast(error.message);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { createProduct, isLoading, error };
};
