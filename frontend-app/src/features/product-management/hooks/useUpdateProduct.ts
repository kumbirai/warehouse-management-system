import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { productService } from '../services/productService';
import { UpdateProductRequest } from '../types/product';
import { logger } from '../../../utils/logger';

export interface UseUpdateProductResult {
  updateProduct: (
    productId: string,
    request: UpdateProductRequest,
    tenantId: string
  ) => Promise<void>;
  isLoading: boolean;
  error: Error | null;
}

export const useUpdateProduct = (): UseUpdateProductResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const navigate = useNavigate();

  const updateProduct = async (
    productId: string,
    request: UpdateProductRequest,
    tenantId: string
  ) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await productService.updateProduct(productId, request, tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to update product');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      logger.info('Product updated successfully', { productId: response.data.productId });
      navigate(`/products/${productId}`);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to update product');
      logger.error('Error updating product:', error);
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { updateProduct, isLoading, error };
};
