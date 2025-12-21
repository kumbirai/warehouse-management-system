import { useCallback, useEffect, useRef, useState } from 'react';
import { productService } from '../services/productService';
import { Product, ProductListFilters } from '../types/product';
import { logger } from '../../../utils/logger';

export interface UseProductsResult {
  products: Product[];
  isLoading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
  pagination?: {
    currentPage: number;
    totalPages: number;
    totalItems: number;
    itemsPerPage: number;
  };
}

export const useProducts = (filters: ProductListFilters): UseProductsResult => {
  const [products, setProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const [pagination, setPagination] = useState<
    | {
        currentPage: number;
        totalPages: number;
        totalItems: number;
        itemsPerPage: number;
      }
    | undefined
  >(undefined);
  const abortControllerRef = useRef<AbortController | null>(null);
  const isFetchingRef = useRef(false);

  const fetchProducts = useCallback(async () => {
    // Prevent concurrent fetches
    if (isFetchingRef.current) {
      return;
    }

    if (!filters.tenantId) {
      if (import.meta.env.DEV) {
        logger.warn('Cannot fetch products: tenantId is missing', {
          filters,
          hasTenantId: !!filters.tenantId,
        });
      }
      setIsLoading(false);
      setProducts([]);
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

      // Don't check for abort here - the request already completed successfully
      // Even if cleanup aborted the signal, we should process the response
      // The abort signal is only for canceling in-flight requests, not for ignoring completed ones

      if (response.error) {
        throw new Error(response.error.message || 'Failed to fetch products');
      }

      // Validate response structure
      if (!response.data) {
        logger.error('Invalid response from server: missing data field', {
          response,
          responseType: typeof response,
          responseKeys: response ? Object.keys(response) : [],
        });
        throw new Error('Invalid response from server: missing data field');
      }

      // Additional validation: ensure data has expected structure
      if (typeof response.data !== 'object' || response.data === null) {
        logger.error('Invalid response data type: expected object', {
          dataType: typeof response.data,
          dataValue: response.data,
        });
        throw new Error('Invalid response from server: data is not an object');
      }

      // Extract products array from the nested response structure
      // Backend returns ApiResponse<ProductListQueryResult> where ProductListQueryResult has a 'products' property
      // Handle both direct array and nested structure
      let productsArray: Product[] = [];

      if (response.data) {
        if (Array.isArray(response.data.products)) {
          productsArray = response.data.products;
        } else if (Array.isArray(response.data)) {
          // Handle case where data is directly an array (shouldn't happen but be defensive)
          productsArray = response.data;
        } else {
          logger.warn('Products is not an array in response', {
            productsType: typeof response.data.products,
            productsValue: response.data.products,
            responseData: response.data,
            responseDataKeys: response.data ? Object.keys(response.data) : [],
          });
        }
      }

      // Validate that we have products or log a warning
      if (productsArray.length === 0 && response.data?.totalCount && response.data.totalCount > 0) {
        logger.warn('Total count indicates products exist but array is empty', {
          totalCount: response.data.totalCount,
          responseData: response.data,
        });
      }

      // Always set products even if aborted - the request completed successfully
      // The abort signal is just for cleanup, not for preventing state updates
      setProducts(productsArray);

      // Extract pagination metadata
      if (response.data.totalCount !== undefined) {
        const totalItems = response.data.totalCount;
        const itemsPerPage = response.data.size || filters.size || 100;
        const currentPage = (response.data.page ?? filters.page ?? 0) + 1; // Convert 0-based to 1-based
        const totalPages = Math.ceil(totalItems / itemsPerPage);

        setPagination({
          currentPage,
          totalPages,
          totalItems,
          itemsPerPage,
        });
      } else {
        setPagination(undefined);
      }
    } catch (err) {
      // Ignore abort errors
      if (err instanceof Error && err.name === 'AbortError') {
        return;
      }

      const error = err instanceof Error ? err : new Error('Failed to fetch products');
      logger.error('Error fetching products:', {
        error,
        errorMessage: error.message,
        errorStack: error.stack,
        errorName: error.name,
        filters,
        errorDetails:
          err instanceof Error
            ? {
                name: err.name,
                message: err.message,
                stack: err.stack,
              }
            : String(err),
      });
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

  return { products, isLoading, error, refetch: fetchProducts, pagination };
};
