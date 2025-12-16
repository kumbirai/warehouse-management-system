import { useState, useEffect } from 'react';
import { productService } from '../services/productService';
import { Product } from '../types/product';
import { logger } from '../../../utils/logger';

export interface UseProductResult {
  product: Product | null;
  isLoading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
}

export const useProduct = (productId: string, tenantId: string): UseProductResult => {
  const [product, setProduct] = useState<Product | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchProduct = async () => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await productService.getProduct(productId, tenantId);
      
      if (response.success && response.data) {
        setProduct(response.data);
      } else {
        throw new Error(response.error?.message || 'Failed to fetch product');
      }
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to fetch product');
      logger.error('Error fetching product:', error);
      setError(error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (productId && tenantId) {
      fetchProduct();
    }
  }, [productId, tenantId]);

  return { product, isLoading, error, refetch: fetchProduct };
};

