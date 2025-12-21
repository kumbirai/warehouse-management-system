import { useNavigate } from 'react-router-dom';
import { ProductForm } from '../components/ProductForm';
import { useCreateProduct } from '../hooks/useCreateProduct';
import { CreateProductRequest, UpdateProductRequest } from '../types/product';
import { useAuth } from '../../../hooks/useAuth';
import { FormPageLayout } from '../../../components/layouts';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';

export const ProductCreatePage = () => {
  const navigate = useNavigate();
  const { createProduct, isLoading, error } = useCreateProduct();
  const { user } = useAuth();

  const handleSubmit = async (values: CreateProductRequest | UpdateProductRequest) => {
    if (!user?.tenantId) {
      throw new Error('Tenant ID is required');
    }
    // Type guard to ensure we have CreateProductRequest
    if ('productCode' in values) {
      await createProduct(values, user.tenantId);
    } else {
      throw new Error('Product code is required for creation');
    }
  };

  const handleCancel = () => {
    navigate(Routes.products);
  };

  return (
    <FormPageLayout
      breadcrumbs={getBreadcrumbs.productCreate()}
      title="Create Product"
      description="Create a new product with barcode and unit of measure"
      error={error?.message || null}
      maxWidth="md"
    >
      <ProductForm onSubmit={handleSubmit} onCancel={handleCancel} isSubmitting={isLoading} />
    </FormPageLayout>
  );
};
