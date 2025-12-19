import { useCallback, useEffect, useState } from 'react';
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

  const fetchProducts = useCallback(async () => {
    if (!filters.tenantId) {
      return;
    }

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

      // Extract products array from the nested response structure
      // Backend returns ApiResponse<ProductListQueryResult> where ProductListQueryResult has a 'products' property
      const productsArray = Array.isArray(response.data.products) ? response.data.products : [];

      setProducts(productsArray);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to fetch products');
      logger.error('Error fetching products:', error);
      setError(error);
      setProducts([]); // Reset products on error
    } finally {
      setIsLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  return { products, isLoading, error, refetch: fetchProducts };
};
