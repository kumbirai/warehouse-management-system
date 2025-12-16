import { useEffect, useState } from 'react';
import { productService } from '../services/productService';
import { Product, ProductListFilters } from '../types/product';
import { logger } from '../../../utils/logger';

export interface UseProductsResult {
  products: Product[];
  isLoading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
}

export const useProducts = (filters: ProductListFilters): UseProductsResult => {
  const [products, setProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchProducts = async () => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await productService.listProducts(filters);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to fetch products');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      setProducts(response.data);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to fetch products');
      logger.error('Error fetching products:', error);
      setError(error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (filters.tenantId) {
      fetchProducts();
    }
  }, [
    filters.tenantId,
    filters.page,
    filters.size,
    filters.category,
    filters.brand,
    filters.search,
  ]);

  return { products, isLoading, error, refetch: fetchProducts };
};
