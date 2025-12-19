import { Container } from '@mui/material';
import { useNavigate, useParams } from 'react-router-dom';
import { ProductDetail } from '../components/ProductDetail';
import { useProduct } from '../hooks/useProduct';
import { useAuth } from '../../../hooks/useAuth';

export const ProductDetailPage = () => {
  const { productId } = useParams<{ productId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  // Call hooks unconditionally before any early returns
  const { product, isLoading, error } = useProduct(productId || '', user?.tenantId || '');

  if (!productId) {
    navigate('/products');
    return null;
  }

  if (!user?.tenantId) {
    return <div>Tenant ID is required</div>;
  }

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <ProductDetail product={product} isLoading={isLoading} error={error} />
    </Container>
  );
};
