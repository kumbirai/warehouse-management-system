import { useCallback, useEffect, useState, useRef } from 'react';
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
  const abortControllerRef = useRef<AbortController | null>(null);
  const isFetchingRef = useRef(false);

  const fetchProducts = useCallback(async () => {
    // Prevent concurrent fetches
    if (isFetchingRef.current) {
      return;
    }

    if (!filters.tenantId) {
      setIsLoading(false);
      return;
    }

    // Cancel any ongoing request
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }

    // Create new abort controller for this request
    abortControllerRef.current = new AbortController();
    isFetchingRef.current = true;
    setIsLoading(true);
    setError(null);

    try {
      const response = await productService.listProducts(filters);

      // Check if request was aborted
      if (abortControllerRef.current?.signal.aborted) {
        return;
      }

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
      // Ignore abort errors
      if (err instanceof Error && err.name === 'AbortError') {
        return;
      }

      const error = err instanceof Error ? err : new Error('Failed to fetch products');
      logger.error('Error fetching products:', error);
      setError(error);
      setProducts([]); // Reset products on error
    } finally {
      isFetchingRef.current = false;
      setIsLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    fetchProducts();

    // Cleanup: abort request on unmount or when filters change
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, [fetchProducts]);

  return { products, isLoading, error, refetch: fetchProducts };
};
