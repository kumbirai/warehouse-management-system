import { useNavigate, useParams } from 'react-router-dom';
import { useMemo } from 'react';
import { ProductForm } from '../components/ProductForm';
import { useUpdateProduct } from '../hooks/useUpdateProduct';
import { useProduct } from '../hooks/useProduct';
import { CreateProductRequest, UpdateProductRequest } from '../types/product';
import { useAuth } from '../../../hooks/useAuth';
import { FormPageLayout } from '../../../components/layouts';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';

export const ProductEditPage = () => {
  const { productId } = useParams<{ productId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { updateProduct, isLoading, error } = useUpdateProduct();

  // Fetch product for default values
  const { product, isLoading: isLoadingProduct } = useProduct(
    productId || '',
    user?.tenantId || ''
  );

  if (!productId) {
    navigate(Routes.products);
    return null;
  }

  if (!user?.tenantId) {
    return (
      <FormPageLayout
        breadcrumbs={getBreadcrumbs.productList()}
        title="Edit Product"
        error="Tenant ID is required to edit products"
      >
        <div />
      </FormPageLayout>
    );
  }

  // TypeScript narrowing: productId and user.tenantId are guaranteed to be non-null after checks
  const validProductId = productId;
  const validTenantId = user.tenantId;

  const handleSubmit = async (values: CreateProductRequest | UpdateProductRequest) => {
    // Type guard to ensure we have UpdateProductRequest (no productCode)
    if ('productCode' in values) {
      throw new Error('Product code cannot be updated');
    }
    await updateProduct(validProductId, values, validTenantId);
  };

  const handleCancel = () => {
    navigate(Routes.productDetail(validProductId));
  };

  // Convert Product to form default values - memoized to prevent unnecessary re-renders
  const defaultValues = useMemo(() => {
    if (!product) return undefined;
    return {
      productCode: product.productCode,
      description: product.description,
      primaryBarcode: product.primaryBarcode,
      unitOfMeasure: product.unitOfMeasure,
      secondaryBarcodes: product.secondaryBarcodes || [],
      category: product.category || '',
      brand: product.brand || '',
    };
  }, [product]);

  return (
    <FormPageLayout
      breadcrumbs={getBreadcrumbs.productEdit()}
      title="Edit Product"
      description="Update product information"
      error={error?.message || null}
      maxWidth="md"
    >
      {isLoadingProduct ? (
        <div>Loading product data...</div>
      ) : (
        <ProductForm
          key={product?.productId}
          defaultValues={defaultValues}
          onSubmit={handleSubmit}
          onCancel={handleCancel}
          isSubmitting={isLoading || isLoadingProduct}
          isUpdate={true}
        />
      )}
    </FormPageLayout>
  );
};
