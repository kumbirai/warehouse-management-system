import { Container } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { ProductForm } from '../components/ProductForm';
import { useCreateProduct } from '../hooks/useCreateProduct';
import { CreateProductRequest } from '../types/product';
import { useAuth } from '../../../hooks/useAuth';

export const ProductCreatePage = () => {
  const navigate = useNavigate();
  const { createProduct, isLoading } = useCreateProduct();
  const { user } = useAuth();

  const handleSubmit = async (values: CreateProductRequest) => {
    if (!user?.tenantId) {
      throw new Error('Tenant ID is required');
    }
    await createProduct(values, user.tenantId);
  };

  const handleCancel = () => {
    navigate('/products');
  };

  return (
    <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
      <ProductForm
        onSubmit={handleSubmit}
        onCancel={handleCancel}
        isSubmitting={isLoading}
      />
    </Container>
  );
};

